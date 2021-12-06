package com.amazon.ionelement

import com.amazon.ion.IonBlob
import com.amazon.ion.IonList
import com.amazon.ion.IonNull
import com.amazon.ion.IonString
import com.amazon.ion.IonBool
import com.amazon.ion.IonClob
import com.amazon.ion.IonInt
import com.amazon.ion.IonSexp
import com.amazon.ion.IonStruct
import com.amazon.ion.IonSymbol
import com.amazon.ion.IonValue
import java.math.BigInteger


@ExperimentalUnsignedTypes
fun IonValue.toIonBinary(symbols: MutableList<String>): IonBinaryValue {
    val unannotatedValue = when (this) {
        is IonSymbol -> this.toIonBinary(symbols)
        is IonString -> this.toIonBinary()
        is IonBool -> this.toIonBinary()
        is IonList -> this.toIonBinary(symbols)
        is IonStruct -> this.toIonBinary(symbols)
        is IonNull -> this.toIonBinary()
        is IonInt -> this.toIonBinary()
        is IonClob -> this.toIonBinary()
        is IonBlob -> this.toIonBinary()
        is IonSexp -> this.toIonBinary(symbols)
        else -> TODO("Floats, Decimals, Timestamps not supported yet")
    }

    if (this.typeAnnotationSymbols.isEmpty()) {
        return unannotatedValue
    } else {
        val annotationSids = this.typeAnnotationSymbols.map { VarUInt(internSymbol(it.text, symbols)) }
        val annotationLength = VarUInt(annotationSids.sumBy { it.size })
        val length = annotationLength.size + annotationSids.size + unannotatedValue.size
        return if (length < 14) {
            val tl = 0xE0.toUByte() or length.toUByte()
            IonBinaryAnnotatedValue(
                td = tl,
                annot_length = annotationLength,
                annot = annotationSids,
                value = unannotatedValue
            )
        } else {
            IonBinaryAnnotatedValue(
                td = 0xEE_u,
                length = VarUInt(length),
                annot_length = annotationLength,
                annot = annotationSids,
                value = unannotatedValue
            )
        }
    }
}

@ExperimentalUnsignedTypes
fun localSymbolTableOf(symbols: List<String>): LocalSymbolTable? {
    if (symbols.isEmpty()) return null

    val symbolsList = symbols.toIonBinary()

    val childValues = listOf(IonBinaryStructField(VarUInt(7), symbolsList))
    val symTabLen = childValues.sumBy { it.size }
    val symbolTable = IonBinaryContainer(
        td = if (symTabLen < 14) (0xD0.toUByte() or symTabLen.toUByte()) else 0xDE_u,
        length = if (symTabLen < 14) null else VarUInt(symTabLen),
        values = childValues
    )
    val len = symbolTable.size + 2 // for the annotation length VarUInt and the one annotation
    val lst = IonBinaryAnnotatedValue(
        td = if (len < 14) (0xE0.toUByte() or len.toUByte()) else 0xEE_u,
        length = if (len < 14) null else VarUInt(len),
        annot_length = VarUInt(1),
        annot = listOf(VarUInt(3)),
        value = symbolTable
    )
    return LocalSymbolTable(symbols, lst)
}

@ExperimentalUnsignedTypes
fun List<String>.toIonBinary(): IonBinaryValue {
    val childValues = map { it.toIonBinary() }
    val length = childValues.sumBy { it.size }
    return if (length < 14) {
        val tl = 0xB0.toUByte() or length.toUByte()
        IonBinaryContainer(tl, values = childValues)
    } else {
        IonBinaryContainer(0xBE_u, VarUInt(length), childValues)
    }
}

@ExperimentalUnsignedTypes
fun String.toIonBinary(): IonBinaryValue {
    val stringValueBytes = this.toByteArray().toUByteArray()
    return when {
        stringValueBytes.isEmpty() -> IonBinaryScalar(0x80_u)
        stringValueBytes.size < 14 -> {
            val tl = 0x80.toUByte() or stringValueBytes.size.toUByte()
            IonBinaryScalar(tl, value = stringValueBytes)
        }
        else -> IonBinaryScalar(0x8E_u, VarUInt(stringValueBytes.size), stringValueBytes)
    }
}


@ExperimentalUnsignedTypes
fun IonStruct.toIonBinary(symbols: MutableList<String>): IonBinaryValue {
    if (isNullValue) return IonBinaryContainer(0xDF_u, values = emptyList())

    val childValues = map {
        val sid = internSymbol(it.fieldName, symbols)
        IonBinaryStructField(VarUInt(sid), it.toIonBinary(symbols))
    }
    val length = childValues.sumBy { it.size }
    return if (length < 14) {
        val tl = 0xD0.toUByte() or length.toUByte()
        IonBinaryContainer(tl, values = childValues)
    } else {
        IonBinaryContainer(0xDE_u, VarUInt(length), childValues)
    }
}

@ExperimentalUnsignedTypes
fun IonList.toIonBinary(symbols: MutableList<String>): IonBinaryValue {
    if (isNullValue) return IonBinaryContainer(0xBF_u, values = emptyList())

    val childValues = map { it.toIonBinary(symbols) }
    val length = childValues.sumBy { it.size }
    return if (length < 14) {
        val tl = 0xB0.toUByte() or length.toUByte()
        IonBinaryContainer(tl, values = childValues)
    } else {
        IonBinaryContainer(0xBE_u, VarUInt(length), childValues)
    }
}

@ExperimentalUnsignedTypes
fun IonSexp.toIonBinary(symbols: MutableList<String>): IonBinaryValue {
    if (isNullValue) return IonBinaryContainer(0xCF_u, values = emptyList())

    val childValues = map { it.toIonBinary(symbols) }
    val length = childValues.sumBy { it.size }
    return if (length < 14) {
        val tl = 0xC0.toUByte() or length.toUByte()
        IonBinaryContainer(tl, values = childValues)
    } else {
        IonBinaryContainer(0xCE_u, VarUInt(length), childValues)
    }
}

@ExperimentalUnsignedTypes
fun IonNull.toIonBinary(): IonBinaryValue = IonBinaryScalar(0x0F_u)

@ExperimentalUnsignedTypes
fun IonString.toIonBinary(): IonBinaryValue {
    val stringValueBytes = this.takeUnless { it.isNullValue }?.stringValue()?.toByteArray()?.toUByteArray()
    return when {
        stringValueBytes == null -> IonBinaryScalar(0x8F_u)
        stringValueBytes.isEmpty() -> IonBinaryScalar(0x80_u)
        stringValueBytes.size < 14 -> {
            val tl = 0x80.toUByte() or stringValueBytes.size.toUByte()
            IonBinaryScalar(tl, value = stringValueBytes)
        }
        else -> IonBinaryScalar(0x8E_u, VarUInt(stringValueBytes.size), stringValueBytes)
    }
}

@ExperimentalUnsignedTypes
fun IonClob.toIonBinary(): IonBinaryValue {
    val bytes = this.takeUnless { it.isNullValue }?.bytes?.toUByteArray()
    return when {
        bytes == null -> IonBinaryScalar(0x9F_u)
        bytes.isEmpty() -> IonBinaryScalar(0x90_u)
        bytes.size < 14 -> {
            val tl = 0x90.toUByte() or bytes.size.toUByte()
            IonBinaryScalar(tl, value = bytes)
        }
        else -> IonBinaryScalar(0x9E_u, VarUInt(bytes.size), bytes)
    }
}



@ExperimentalUnsignedTypes
fun IonBlob.toIonBinary(): IonBinaryValue {
    val bytes = this.takeUnless { it.isNullValue }?.bytes?.toUByteArray()
    return when {
        bytes == null -> IonBinaryScalar(0xAF_u)
        bytes.isEmpty() -> IonBinaryScalar(0xA0_u)
        bytes.size < 14 -> {
            val tl = 0xA0.toUByte() or bytes.size.toUByte()
            IonBinaryScalar(tl, value = bytes)
        }
        else -> IonBinaryScalar(0xAE_u, VarUInt(bytes.size), bytes)
    }
}

@ExperimentalUnsignedTypes
fun BigInteger.toUByteArray(): UByteArray {
    return this.abs().toByteArray().dropWhile { it == 0.toByte() }.toByteArray().toUByteArray()
}

@ExperimentalUnsignedTypes
fun IonInt.toIonBinary(): IonBinaryValue {
    return if (isNullValue) {
        IonBinaryScalar(0x2F_u)
    } else {
        val bytes = bigIntegerValue().toUByteArray()
        val typeId = (if (bigIntegerValue().signum() < 0) 0x30 else 0x20).toUByte()
        when {
            bytes.isEmpty() -> IonBinaryScalar(0x20_u)
            bytes.size < 14 -> IonBinaryScalar(typeId or bytes.size.toUByte(), value = bytes)
            else -> IonBinaryScalar(typeId or 0x0E_u, VarUInt(bytes.size), bytes)
        }
    }
}

val ION_1_0_SYM_TAB = listOf(
    "\$ion", "\$ion_1_0", "\$ion_symbol_table",
    "name", "version", "imports",
    "symbols", "max_id", "\$ion_shared_symbol_table"
)
fun internSymbol(symbol: String, lst: MutableList<String>): Int {
    return when (val sid = ION_1_0_SYM_TAB.indexOf(symbol)) {
        -1 -> when (val i = lst.indexOf(symbol)) {
            -1 -> {
                lst.add(symbol)
                lst.size + 9
            }
            else -> i + 10
        }
        else -> sid + 1
    }
}

@ExperimentalUnsignedTypes
fun IonSymbol.toIonBinary(symbols: MutableList<String>): IonBinaryValue {
    if (this.isNullValue) return IonBinaryScalar(0x7F_u)
    val sid = internSymbol(this.stringValue(), symbols)
    val bytes = sid.toIonUInt()

    return when {
        bytes.isEmpty() -> IonBinaryScalar(0x70_u)
        bytes.size < 14 -> {
            val tl = 0x70.toUByte() or bytes.size.toUByte()
            IonBinaryScalar(tl, value = bytes)
        }
        else -> IonBinaryScalar(0x7E_u, VarUInt(bytes.size), bytes)
    }
}

@ExperimentalUnsignedTypes
private val BOOL_NULL = IonBinaryScalar(0x1F_u)
@ExperimentalUnsignedTypes
private val BOOL_TRUE = IonBinaryScalar(0x11_u)
@ExperimentalUnsignedTypes
private val BOOL_FALSE = IonBinaryScalar(0x10_u)
@ExperimentalUnsignedTypes
fun IonBool.toIonBinary(): IonBinaryValue {
    return when {
        isNullValue -> BOOL_NULL
        booleanValue() -> BOOL_TRUE
        else -> BOOL_FALSE
    }
}

// TODO: Calculate num bytes needed ahead of time
@ExperimentalUnsignedTypes
fun Int.toIonUInt(): UByteArray {
    val bytes = UByteArray(Int.SIZE_BYTES)
    for (i in 0 until Int.SIZE_BYTES) {
        bytes[Int.SIZE_BYTES - 1 - i] = ((this shr 8 * i) and 0xFF).toUByte()
    }
    return bytes.dropWhile { it <= 0u  }.toUByteArray()
}
