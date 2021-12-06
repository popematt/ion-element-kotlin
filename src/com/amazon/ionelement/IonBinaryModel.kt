package com.amazon.ionelement

import com.amazon.ion.IonSystem

@ExperimentalUnsignedTypes
interface IonBinaryValue {
    val size: Int
    fun flatten(): UByteArray = UByteArray(size).also { this.copyInto(it) }
    fun copyInto(byteArray: UByteArray, position: Int = 0): Int
}


@ExperimentalUnsignedTypes
class LocalSymbolTable(val symbols: List<String>, val ionBinary: IonBinaryAnnotatedValue)

@ExperimentalUnsignedTypes
class IonBinaryDatagram(val localSymbolTable: LocalSymbolTable?, val values: List<IonBinaryValue>): IonBinaryValue {
    companion object {
        val IVM = ubyteArrayOf(0xE0_u, 0x01_u, 0x00_u, 0xEA_u)
    }

    override val size: Int = 4 + (localSymbolTable?.ionBinary?.size ?: 0) + values.sumBy { it.size }

    override fun copyInto(byteArray: UByteArray, position: Int): Int {
        var pos = position
        pos = IVM.copyInto(byteArray, pos)
        pos = localSymbolTable?.ionBinary?.copyInto(byteArray, pos) ?: pos
        values.forEach { pos = it.copyInto(byteArray, pos) }
        return pos
    }
}


@ExperimentalUnsignedTypes
class IonBinaryStructField(val fieldName: VarUInt, val value: IonBinaryValue): IonBinaryValue {
    override val size: Int = fieldName.size + value.size
    override fun copyInto(byteArray: UByteArray, position: Int): Int {
        val pos = fieldName.copyInto(byteArray, position)
        return value.copyInto(byteArray, pos)
    }
}

@ExperimentalUnsignedTypes
class IonBinaryContainer constructor(val td: UByte, val length: VarUInt? = null, val values: List<IonBinaryValue>): IonBinaryValue {
    override val size: Int = 1 + length.size + values.map { it.size }.sum()
    override fun copyInto(byteArray: UByteArray, position: Int): Int {
        var pos = position
        pos = td.copyInto(byteArray, pos)
        pos = length?.copyInto(byteArray, pos) ?: pos
        values.forEach { pos = it.copyInto(byteArray, pos) }
        return pos
    }
}

@ExperimentalUnsignedTypes
class IonBinaryAnnotatedValue constructor(val td: UByte, val length: VarUInt? = null, val annot_length: VarUInt, val annot: List<VarUInt>, val value: IonBinaryValue): IonBinaryValue {
    override val size: Int = 1 + length.size + annot_length.size + annot.map { it.size }.sum() + value.size
    override fun copyInto(byteArray: UByteArray, position: Int): Int {
        var pos = position
        pos = td.copyInto(byteArray, pos)
        pos = length?.copyInto(byteArray, pos) ?: pos
        pos = annot_length.copyInto(byteArray, pos)
        annot.forEach { pos = it.copyInto(byteArray, pos) }
        pos = value.copyInto(byteArray, pos)
        return pos
    }
}

@ExperimentalUnsignedTypes
class IonBinaryScalar(val td: UByte, val length: VarUInt? = null, val value: UByteArray = EMPTY_BYTE_ARRAY): IonBinaryValue {
    override val size: Int = 1 + length.size + value.size
    override fun copyInto(byteArray: UByteArray, position: Int): Int {
        var pos = position
        pos = td.copyInto(byteArray, pos)
        pos = length?.copyInto(byteArray, pos) ?: pos
        pos = value.copyInto(byteArray, pos)
        return pos
    }
}

@ExperimentalUnsignedTypes
val VarUInt?.size: Int
    get() = this?.size ?: 0

@ExperimentalUnsignedTypes
class VarUInt(val value: Long): IonBinaryValue {
    constructor(value: Int): this(value.toLong())
    init { require(value >= 0L) }

    companion object {
        fun calculateVarUIntSize(value: Long): Int {
            if (value == 0L) return 1
            var bytes = 0
            while (value ushr (bytes * 7) > 0) bytes++
            return bytes
        }
    }

    override val size: Int = calculateVarUIntSize(value)

    override fun copyInto(byteArray: UByteArray, position: Int): Int {
        if (value == 0L) { byteArray[position] = 0x70_u; return position + 1 }

        for (i in size downTo 1) {
            byteArray[position + size - i] = (value ushr (7 * (i - 1)) and 0x7F).toUByte()
        }
        byteArray[position + size - 1] = ((value and 0x7F) or 0x80).toUByte()
        return position + size
    }
}
