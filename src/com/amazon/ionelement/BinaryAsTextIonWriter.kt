package com.amazon.ionelement

import com.amazon.ion.IonType

object Ansi {
    const val RESET = "\u001B[0m"
    const val BLACK = "\u001B[30m"
    const val RED = "\u001B[31m"
    const val LIGHT_RED = "\u001B[91m"
    const val LIGHT_RED_BACKGROUND = "\u001B[101m"
    const val LIGHT_PINK = "\u001B[38;5;204m"
    const val MEDIUM_GRAY = "\u001B[38;5;242m"
    const val LIGHT_PINK_BACKGROUND = "\u001B[48;5;217m"
    const val GREEN = "\u001B[32m"
    const val YELLOW = "\u001B[33m"
    const val BLUE = "\u001B[34m"
    const val PURPLE = "\u001B[35m"
    const val CYAN = "\u001B[36m"
    const val WHITE = "\u001B[97m"
    const val BLACK_BACKGROUND = "\u001B[40m"
    const val RED_BACKGROUND = "\u001B[41m"
    const val GREEN_BACKGROUND = "\u001B[42m"
    const val YELLOW_BACKGROUND = "\u001B[43m"
    const val BLUE_BACKGROUND = "\u001B[44m"
    const val PURPLE_BACKGROUND = "\u001B[45m"
    const val CYAN_BACKGROUND = "\u001B[46m"
    const val WHITE_BACKGROUND = "\u001B[47m"
    const val LIGHT_BLUE_BACKGROUND = "\u001B[104m"
    const val BOLD = "\u001B[1m"
    const val ITALIC = "\u001B[3m"
    const val UNDERLINE = "\u001B[4m"
}

/*
Take notes about COW stuff. What was frustrating, what worked well.

- Good:
  - Kotlin's unsigned integer types are nice
  - Kotlin allows underscored in numbers, so instead of 0xFU, we can write 0xF_u

- Bad:
  - IonJava has crazy complicated inheritance for IonReader and IonWriter. Interface for those have so many methods, it's difficult to write one.
  - SymbolTables are hard to use. How can we do it differently? It was easier to just write my own (simplified) symbol table.
  - Timestamp format seems unnecessarily complicated.

- Idea: Utility method to calculate the binary size of an IonValue / IonElement

 */

class IonBinaryTextLine(val td: Byte?, val len: ByteArray?, val value: ByteArray?, val childLines: List<IonBinaryTextLine>?) {
    fun size(): Int = (if (td == null) 0 else 1) + (len?.size ?: 0) + (value?.size ?:0) + (childLines?.map { it.size() }?.sum() ?: 0)
}

data class TextFormat(
    val typeMarker: Pair<String, String> = NONE,
    val varUIntLength: Pair<String, String> = NONE,
    val varUIntSid: Pair<String, String> = NONE,
    val ivm: Pair<String, String> = NONE,
    val punctuation: Pair<String, String> = NONE,
    val postProcessor: (String) -> String = { it }
) {
    companion object {
        val NONE = "" to ""
        val ANSI_COLOR = TextFormat(
            typeMarker = Ansi.LIGHT_RED to Ansi.RESET,
            varUIntLength = Ansi.LIGHT_PINK to Ansi.RESET,
            varUIntSid = Ansi.BLUE to Ansi.RESET,
            ivm = Ansi.GREEN to Ansi.RESET,
            punctuation = Ansi.MEDIUM_GRAY to Ansi.RESET
        )
        val ANSI_BACKGROUND = TextFormat(
            typeMarker = Ansi.LIGHT_RED_BACKGROUND to Ansi.RESET,
            varUIntLength = Ansi.LIGHT_PINK_BACKGROUND to Ansi.RESET,
            varUIntSid = Ansi.LIGHT_BLUE_BACKGROUND to Ansi.RESET,
            ivm = Ansi.GREEN_BACKGROUND + Ansi.WHITE to Ansi.RESET
        )
        val ANSI_FONTS = TextFormat(
            typeMarker = Ansi.BOLD to Ansi.RESET,
            varUIntLength = Ansi.ITALIC to Ansi.RESET,
            varUIntSid = Ansi.UNDERLINE to Ansi.RESET
        )
        val ANSI_FONTS_COLORS = TextFormat(
            typeMarker = Ansi.BOLD + Ansi.LIGHT_RED to Ansi.RESET,
            varUIntLength = Ansi.ITALIC + Ansi.LIGHT_PINK to Ansi.RESET,
            varUIntSid = Ansi.UNDERLINE + Ansi.BLUE to Ansi.RESET,
            ivm = Ansi.GREEN to Ansi.RESET,
            punctuation = Ansi.MEDIUM_GRAY to Ansi.RESET
        )
        val HTML_FONTS = TextFormat(
            typeMarker = "<b>" to "</b>",
            varUIntLength = "<i>" to "</i>",
            varUIntSid = "<ins>" to "</ins>"
        )
        val HTML_BACKGROUNDS = TextFormat(
            ivm = """<span_style="background-color:LightGray;">""" to "</span>",
            typeMarker = """<span_style="background-color:Tomato;">""" to "</span>",
            varUIntLength = """<span_style="background-color:LightPink;">""" to "</span>",
            varUIntSid = """<span_style="background-color:SkyBlue;">""" to "</span>",
            punctuation = """<span_style="color:Gray;">""" to "</span>",
            postProcessor = { it.replace("_", " ") }
        )
        val HTML_BORDERS = TextFormat(
            ivm = """<span_style="border:1px_solid_DarkGray;">""" to "</span>",
            typeMarker = """<span_style="border:1px_solid_Tomato;">""" to "</span>",
            varUIntLength = """<span_style="border:1px_solid_LightPink;">""" to "</span>",
            varUIntSid = """<span_style="border:1px_solid_DodgerBlue;">""" to "</span>",
            punctuation = """<span_style="color:Gray;">""" to "</span>",
            postProcessor = { it.replace("_", " ") }
        )
    }
}

data class PrettyPrintOptions(
    /** Only applicable when [multiline] is true. */
    val indent: String = "  ",
    val multiline: Boolean = true,
    val wrapLongLines: Boolean = true,
    val octetsPerWord: Int = 8,
    val wordsPerLine: Int = 2,
    /** Only applicable when [multiline] is true; TODO - see if we can support this for multiline = false */
    val delimitingPunctuation: Boolean = false,
    val textFormatting: TextFormat = TextFormat()
)
private fun PrettyPrintOptions.apply(type: (TextFormat) -> Pair<String, String>, string: String): String = with (type(this.textFormatting)) { "$first$string$second" }
fun PrettyPrintOptions.legend(): String =
    textFormatting.postProcessor.invoke(apply(TextFormat::ivm, "ivm") + " " +
        apply(TextFormat::typeMarker, "type") + " " +
        apply(TextFormat::varUIntLength, "var_uint_length") + " " +
        apply(TextFormat::varUIntSid, "var_uint_sid"))

@ExperimentalUnsignedTypes
fun IonBinaryValue.toPrettyString(options: PrettyPrintOptions): String {
    val data = toPrettyString(options, 0)

    return data.let {
        if (options.wrapLongLines && !options.multiline) {
            it.split(" ").chunked(options.octetsPerWord * options.wordsPerLine).joinToString(separator = "\n") { line ->
                line.chunked(options.octetsPerWord).joinToString("   ") { word ->
                    word.joinToString(separator = " ")
                }
            }
        } else {
            it
        }
    }.let { options.textFormatting.postProcessor.invoke(it) }
}

@ExperimentalUnsignedTypes
private fun wrapWithContainerDelimiters(type: UByte, contents: String, options: PrettyPrintOptions): String {
    val f = { it: String -> options.apply(TextFormat::punctuation, it) }
    return when (type and 0xF0_u) {
        0xB0.toUByte() -> f("[") + contents + f("]")
        0xC0.toUByte() -> f("(") + contents + f(")")
        0xD0.toUByte() -> f("{") + contents + f("}")
        else -> TODO("Not reachable")
    }
}

@ExperimentalUnsignedTypes
private fun getType(byte: UByte): IonType? {
    return when (byte.toInt().toUInt() and 0xF0_u) {
        0x00u -> IonType.NULL
        0x10u -> IonType.BOOL
        0x20u, 0x30u -> IonType.INT
        0x40u -> IonType.FLOAT
        0x50u -> IonType.DECIMAL
        0x60u -> IonType.TIMESTAMP
        0x70u -> IonType.SYMBOL
        0x80u -> IonType.STRING
        0x90u -> IonType.CLOB
        0xA0u -> IonType.BLOB
        0xB0u -> IonType.LIST
        0xC0u -> IonType.SEXP
        0xD0u -> IonType.STRUCT
        else -> null
    }
}

@ExperimentalUnsignedTypes
fun getSymbolString(lst: LocalSymbolTable?, sid: VarUInt): String? {
    return sid.value.toInt().let { if (it > 9) lst?.symbols?.getOrNull(it - 10) else ION_1_0_SYM_TAB[it] }
}

@ExperimentalUnsignedTypes
fun IonBinaryValue.toPrettyString(options: PrettyPrintOptions, indentLevel: Int, currentLst: LocalSymbolTable? = null): String {
    return if (options.multiline) {

        when (this) {
            is IonBinaryDatagram -> {
                val ivm = options.apply(TextFormat::ivm, IonBinaryDatagram.IVM.toSpacedOctets())
                val lst = localSymbolTable?.ionBinary?.toPrettyString(options, indentLevel)?.let { "\n|$it" } ?: ""
                "|$ivm$lst\n" + values.joinToString("\n") { it.toPrettyString(options, indentLevel, localSymbolTable) }
            }
            is IonBinaryScalar -> {
                val type = options.apply(TextFormat::typeMarker, td.toOctet())
                if (this.length == null) {
                    """|$type ${value.toSpacedOctets()}""".trimEnd()
                } else {
                    val len = options.apply(TextFormat::varUIntLength, " ${length.flatten().toSpacedOctets()}").trimEnd()
                    """|$type$len
                       |${options.indent}${value.toMultilineOctets(options, indentLevel + 1)}"""
                }
            }
            is IonBinaryContainer -> {
                val type = options.apply(TextFormat::typeMarker, td.toOctet())
                val len = this.length?.let { options.apply(TextFormat::varUIntLength, " ${it.flatten().toSpacedOctets()}").trimEnd() } ?: ""
                if (options.delimitingPunctuation) {
                    val valueString = "\n" + values.joinToString("") { it.toPrettyString(options, indentLevel + 1, currentLst) + options.apply(TextFormat::punctuation, ",") + "\n" } + options.indent.repeat(indentLevel)
                    "|$type$len ${wrapWithContainerDelimiters(td, valueString, options)}"
                } else {
                    "|$type$len\n" + values.joinToString("\n") { it.toPrettyString(options, indentLevel + 1, currentLst) }
                }
            }
            is IonBinaryStructField -> {
                val fieldSid = options.apply(TextFormat::varUIntSid, fieldName.flatten().toSpacedOctets())
                val delim = if (options.delimitingPunctuation) options.apply(TextFormat::punctuation, ":") else ""
                "|$fieldSid$delim\n${value.toPrettyString(options, indentLevel + 1, currentLst)}"
            }
            is IonBinaryAnnotatedValue -> {
                val type = options.apply(TextFormat::typeMarker, td.toOctet())
                val len = this.length?.let { options.apply(TextFormat::varUIntLength, " ${it.flatten().toSpacedOctets()}").trimEnd() } ?: ""
                val annotationLen = options.apply(TextFormat::varUIntLength, annot_length.flatten().toSpacedOctets())
                val annotationSeparator = if (options.delimitingPunctuation) options.apply(TextFormat::punctuation, ", ") else " "
                val annotations = this.annot.joinToString(annotationSeparator) { options.apply(TextFormat::varUIntSid, it.flatten().toOctets()) }
                """|$type$len
                   |${options.indent}$annotationLen $annotations${"\n"}${value.toPrettyString(options, indentLevel + 1, currentLst)}"""
            }
            else -> TODO()
        }.replaceIndentByMargin(newIndent = options.indent.repeat(indentLevel))
    } else {
        when (this) {
            is IonBinaryDatagram -> {
                val ivm = options.apply(TextFormat::ivm, IonBinaryDatagram.IVM.toSpacedOctets()) + " "
                val lst = localSymbolTable?.ionBinary?.toPrettyString(options, indentLevel) ?: ""
                "$ivm$lst" + values.joinToString("") { it.toPrettyString(options, indentLevel) }
            }
            is IonBinaryScalar -> {
                val type = options.apply(TextFormat::typeMarker, td.toOctet()) + " "
                val len = this.length?.let { options.apply(TextFormat::varUIntLength, it.flatten().toSpacedOctets()) + " " } ?: ""
                val payload = value.takeIf { it.isNotEmpty() }?.let { it.toSpacedOctets() + " " } ?: ""
                "$type$len$payload"
            }
            is IonBinaryContainer -> {
                val type = options.apply(TextFormat::typeMarker, td.toOctet()) + " "
                val len = this.length?.let { options.apply(TextFormat::varUIntLength, it.flatten().toSpacedOctets()) + " " } ?: ""
                val contents = values.joinToString(separator = "") { it.toPrettyString(options, indentLevel + 1) }
                "$type$len$contents"
            }
            is IonBinaryStructField -> {
                val fieldSid = options.apply(TextFormat::varUIntSid, fieldName.flatten().toSpacedOctets()) + " "
                "$fieldSid${value.toPrettyString(options, indentLevel + 1)}"
            }
            is IonBinaryAnnotatedValue -> {
                val type = options.apply(TextFormat::typeMarker, td.toOctet()) + " "
                val len = this.length?.let { options.apply(TextFormat::varUIntLength, it.flatten().toSpacedOctets()) + " " } ?: ""
                val annotationLen = options.apply(TextFormat::varUIntLength, annot_length.flatten().toSpacedOctets()) + " "
                val annotations = this.annot.joinToString("") { options.apply(TextFormat::varUIntSid, it.flatten().toSpacedOctets()) + " " }
                "$type$len$annotationLen$annotations${value.toPrettyString(options, indentLevel + 1)}"
            }
            else -> TODO()
        }
    }
}

@ExperimentalUnsignedTypes
fun UByteArray.toMultilineOctets(options: PrettyPrintOptions, indentLevel: Int): String {
    val indent = options.indent.repeat(indentLevel)
    return this.chunked(options.octetsPerWord * options.wordsPerLine).joinToString(separator = "\n$indent") {
            line -> line.chunked(options.octetsPerWord).joinToString("   ") {
            word -> word.joinToString(separator = " ") {
            byte -> byte.toOctet()
    } } }
}


@ExperimentalUnsignedTypes
val EMPTY_BYTE_ARRAY = UByteArray(0)

@ExperimentalUnsignedTypes
fun UByte.copyInto(dest: UByteArray, atPosition: Int): Int {
    dest[atPosition] = this
    return atPosition + 1
}

@ExperimentalUnsignedTypes
fun UByteArray.copyInto(dest: UByteArray, atPosition: Int): Int {
    return if (isNotEmpty()) {
        for (i in 0 until size)
            dest[atPosition + i] = this.elementAt(i)
        atPosition + size
    } else {
        atPosition
    }
}

fun Byte.copyInto(dest: ByteArray, atPosition: Int): Int {
    dest[atPosition] = this
    return atPosition + 1
}

fun ByteArray.copyInto(dest: ByteArray, atPosition: Int): Int {
    return if (isNotEmpty()) {
        copyInto(dest, atPosition)
        atPosition + size
    } else {
        atPosition
    }
}

fun ByteArray.toOctets(): String = joinToString(separator = "") { it.toOctet() }
@ExperimentalUnsignedTypes
fun UByteArray.toOctets(): String = joinToString(separator = "") { it.toOctet() }
@ExperimentalUnsignedTypes
fun UByteArray.toSpacedOctets(): String = joinToString(separator = " ") { it.toOctet() }

fun Byte.toOctet(): String = when {
    this < 0 -> (this + 256).toString(16)
    this < 16 -> "0" + this.toString(16)
    else -> this.toString(16)
}
@ExperimentalUnsignedTypes
fun UByte.toOctet(): String = when {
    this < 16u -> "0" + this.toString(16)
    else -> this.toString(16)
}
fun Int.toOctet(): String = (if (this < 16) "0" else "") + this.toString(16)


