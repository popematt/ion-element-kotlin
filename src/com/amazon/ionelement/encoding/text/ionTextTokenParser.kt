package com.amazon.ionelement.encoding.text

import com.amazon.ion.IonType
import com.amazon.ionelement.Ansi
import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.emptyMetaContainer
import com.amazon.ionelement.api.metaContainerOf
import kotlinx.collections.immutable.toPersistentList

infix fun CharSequence.hasPrefix(other: String): Boolean = this.startsWith(other)

fun buildForest(tokens: List<IonTextToken>): List<IonTextTree> {
    val roots = mutableListOf<IonTextTree>()

    var index = 0
    while (index < tokens.size) {
        var symbolCount = 0
        var delimiterCount = 0
        val annotationsTokens: List<IonTextToken> = if (tokens[index].type is TokenType.PartialValue) {
            tokens.subList(index, tokens.size).takeWhile {
                val result = (it.content == "::" && delimiterCount < symbolCount)
                    || (it.content.startsWith("'") && symbolCount == delimiterCount)
                    || (it.content.first() in unquotedSymbolStartCharacters && symbolCount == delimiterCount)
                    || it.type is TokenType.WhiteSpace
                    || it.type is TokenType.Comment

                if (it.content == "::") delimiterCount++
                if (it.content.startsWith("'")) symbolCount++
                if (it.content.first() in unquotedSymbolStartCharacters) symbolCount++

                result
            }.dropLastWhile { it.type is TokenType.PartialValue || delimiterCount == 0 }
        } else {
            emptyList()
        }
        index += annotationsTokens.size

        if (index >= tokens.size) {
            println("Roots \n${roots.joinToString(" ") { it.toSexpString(1) }}")
            println("Annotations \n${annotationsTokens.debugString()}")
        }

        val ionTextToken = tokens[index]
        val treeNode = when (ionTextToken.type) {
            TokenType.WhiteSpace,
            TokenType.Comment -> IonTextTree.Token(ionTextToken)
            TokenType.NotIon -> IonTextTree.SyntaxError("Not valid Ion", IonTextTree.Token(ionTextToken))
            TokenType.Delimiter,
            TokenType.PartialValue -> {
                with(ionTextToken.content) {
                    when {
                        equals("[") -> {
                            val endIndex = findBalancedParenEnd(tokens, index)
                            buildListTree(tokens.subList(index, endIndex + 1))
                        }
                        equals("(") -> {
                            val endIndex = findBalancedParenEnd(tokens, index)
                            buildSexpTree(tokens.subList(index, endIndex + 1))
                        }
                        equals("{") -> {
                            val endIndex = findBalancedParenEnd(tokens, index)
                            buildStructTree(tokens.subList(index, endIndex + 1))
                        }
                        equals("{{") -> {
                            // Blobs and Clobs
                            val endIndex = tokens.subList(index, tokens.size).indexOfFirst { it.content == "}}" }
                            val contents = tokens.subList(index, endIndex + 2)

                            val ionType = with(contents.filter { it.type is TokenType.PartialValue }) {
                                when {
                                    all { it.content.startsWith("'''") } -> IonType.CLOB
                                    size == 1 && singleOrNull { it.content.startsWith('"') } != null -> IonType.CLOB
                                    else -> IonType.BLOB
                                }
                            }
                            IonTextTree.Value(ionType, contents.map { IonTextTree.Token(it) })
                        }
                        startsWith("'''") -> {
                            // Scan forward until we find something that is not a long string, comment, or whitespace
                            IonTextTree.Value(
                                type = IonType.STRING,
                                children = tokens.subList(index, tokens.size)
                                    .takeWhile { it.content.startsWith("'''") || it.type is TokenType.Comment || it.type is TokenType.WhiteSpace }
                                    .dropLastWhile { !it.content.startsWith("'''") }
                                    .map { IonTextTree.Token(it) }
                            )
                        }
                        startsWith("\"") -> IonTextTree.Value(IonType.STRING, listOf(IonTextTree.Token(ionTextToken)))
                        startsWith("\'") -> IonTextTree.Value(IonType.SYMBOL, listOf(IonTextTree.Token(ionTextToken)))
                        first() in "-0123456789" -> {
                            // Int, Decimal, Float, Timestamp
                            val ionType = when {
                                contains(':') -> IonType.TIMESTAMP
                                contains('x') -> IonType.INT
                                contains('b') -> IonType.INT
                                contains('d') -> IonType.DECIMAL
                                contains('e') -> IonType.FLOAT
                                contains('.') -> IonType.DECIMAL
                                else -> IonType.INT
                            }
                            IonTextTree.Value(ionType, listOf(IonTextTree.Token(ionTextToken)))
                        }
                        equals("null") || startsWith("null.") -> {
                            val ionType = IonType.valueOf(ionTextToken.content.takeLastWhile { it != '.' }.toString().toUpperCase())
                            IonTextTree.Value(ionType, listOf(IonTextTree.Token(ionTextToken)))
                        }
                        equals("true") || equals("false") -> IonTextTree.Value(IonType.BOOL, listOf(IonTextTree.Token(ionTextToken)))
                        first() in unquotedSymbolStartCharacters -> IonTextTree.Value(IonType.SYMBOL, listOf(IonTextTree.Token(ionTextToken)))
                        first() in symbolOperatorCharacters -> IonTextTree.Value(IonType.SYMBOL, listOf(IonTextTree.Token(ionTextToken)))
                        else -> TODO("Need to support case: content=$this \n Current State: ${roots.map { it.toSexpString() }}")
                    }
                }
                // symbol::
                // {
                // [
                // (
                // '''
                // {{
                // IonTextTree.Value(IonType.NULL, tokens.map { IonTextTree.Token(it) })
            }
        }.let {
            if (annotationsTokens.isEmpty()) {
                it
            } else {
                val annotationTreeNodes = annotationsTokens.map { t ->
                    if (t.type == TokenType.PartialValue)
                        IonTextTree.Annotation(t)
                    else
                        IonTextTree.Token(t)
                }
                if (it is IonTextTree.Value) {
                    it.copy(children = annotationTreeNodes + it.children)
                } else {
                    IonTextTree.SyntaxError("Annotations followed by a non-value", annotationTreeNodes + it)
                }
            }
        }



        roots.add(treeNode)
        index += treeNode.traverseTokens().count()
    }
    return roots.toPersistentList()
}


class NestedParensDepthTracker {
    var depth = 0
    operator fun invoke(token: IonTextToken): Int {
        if (token.content in "({[") depth++
        val result = depth
        if (token.content in ")}]") depth--
        return result
    }
}

fun buildListTree(tokens: List<IonTextToken>): IonTextTree {
    val childNodes = mutableListOf<IonTextTree>()
    childNodes.add(IonTextTree.Token(tokens.first()))
    val childDepth = NestedParensDepthTracker()
    val groupings = tokens.subList(1, tokens.size - 1).splitOnElements { childDepth(it) == 0 && it.content == ","  }
    groupings.forEach {
        when (it) {
            is SplitResult.Delimiter -> childNodes.add(IonTextTree.Token(it.element))
            is SplitResult.Group -> {
                var seenOneValue = false
                val child = buildForest(it.group).map { node ->
                    if (node is IonTextTree.Value) {
                        if (seenOneValue) {
                            IonTextTree.SyntaxError("Comma missing between values", node)
                        } else {
                            seenOneValue = true
                            node
                        }
                    } else {
                        node
                    }
                }
                childNodes.addAll(child)
            }
        }
    }
    // TODO: Make sure that we alternate between delimiter tokens and value nodes.
    childNodes.add(IonTextTree.Token(tokens.last()))
    return IonTextTree.Value(IonType.LIST, childNodes.toList())
}

fun buildSexpTree(tokens: List<IonTextToken>): IonTextTree {
    val childNodes = mutableListOf<IonTextTree>()
    childNodes.add(IonTextTree.Token(tokens.first()))
    childNodes.addAll(buildForest(tokens.subList(1, tokens.size - 1)))
    childNodes.add(IonTextTree.Token(tokens.last()))
    return IonTextTree.Value(IonType.SEXP, childNodes.toList())
}

fun List<IonTextToken>.debugString() = map { it.debugString() }.toString()
fun IonTextToken.debugString() = "〈$content〉".replace("\n", "\\n")
fun SplitResult<IonTextToken>.debugString() = when (this) {
    is SplitResult.Group -> group.debugString()
    is SplitResult.Delimiter -> Ansi.BLUE + element.debugString() + Ansi.RESET
}

fun buildStructTree(tokens: List<IonTextToken>): IonTextTree {
    // TODO: Syntax validation
    val structChildNodes = mutableListOf<IonTextTree>()
    structChildNodes.add(IonTextTree.Token(tokens.first()))
    val childDepth = NestedParensDepthTracker()
    // Split on comma delimiter tokens that are not in nested containers
    val groupings = tokens.subList(1, tokens.size - 1).splitOnElements { childDepth(it) == 0 && it.content == "," }

    println("Struct groupings " + groupings.map { it.debugString() })

    groupings.forEach splitResult@{ result ->
        when (result) {
            is SplitResult.Delimiter -> structChildNodes.add(IonTextTree.Token(result.element))
            is SplitResult.Group -> {
                // Anything before the field name goes into the struct, and anything
                // after the value goes in the struct
                val group = result.group
                val fieldChildNodes = mutableListOf<IonTextTree>()
                var hasSeenFieldName = false
                group.forEachIndexed { index, token ->
                    if (!hasSeenFieldName) {
                        if (token.type is TokenType.PartialValue) {
                            fieldChildNodes.add(IonTextTree.FieldName(token))
                            hasSeenFieldName = true
                        } else {
                            structChildNodes.add(IonTextTree.Token(token))
                        }
                    } else {
                        fieldChildNodes.add(IonTextTree.Token(token))
                        if (token.type is TokenType.Delimiter && token.content == ":") {
                            // We have enough info to parse the rest now
                            val remainder = buildForest(group.subList(index + 1, group.size))
                            val valueIndex = remainder.indexOfFirst { it is IonTextTree.Value }
                            fieldChildNodes.addAll(remainder.take(valueIndex + 1))
                            structChildNodes.add(IonTextTree.Field(fieldChildNodes))
                            structChildNodes.addAll(remainder.drop(valueIndex + 1))
                            return@splitResult
                        }
                    }
                }
            }
        }
    }
    structChildNodes.add(IonTextTree.Token(tokens.last()))
    return IonTextTree.Value(IonType.STRUCT, structChildNodes.toList())
}

fun findBalancedParenEnd(tokens: List<IonTextToken>, startIndex: Int): Int {
    val openParen = tokens[startIndex].content
    val closeParen = when (openParen) {
        "{" -> "}"
        "[" -> "]"
        "(" -> ")"
        else -> TODO("Not Supported")
    }
    var depth = 0
    (startIndex until tokens.size).forEach { idx ->
        val token = tokens[idx]
        if (token.content == openParen) depth++
        if (token.content == closeParen) depth--
        if (depth == 0) {
            return idx
        }
    }
    throw IllegalArgumentException("Imbalanced $openParen $closeParen")
}


operator fun String.get(range: IntRange): String = slice(range.first .. minOf(range.last, length-1))
infix fun Int.from(startPos: Int): IntRange = startPos until this + startPos

val DELIMITERS = setOf("{{", "}}", "{", "}", "[", "]", "(", ")", "::", ":", ",")
val unquotedSymbolCharacters = ('a'..'z') + ('A'..'Z') + ('0'..'9') + '_' + '$'
val unquotedSymbolStartCharacters = ('a'..'z') + ('A'..'Z') + '_' + '$'
val symbolOperatorCharacters = "!#%&*+-./;<=>?@^`|~"
val numericStopCharacters = "{}[](),\"' \t\n\r\u000B\u000C"

fun readTokens(text: String): List<IonTextToken> {
    val newlineLocations = text.mapIndexedNotNull { index, c -> if (c == '\n') index else null }
    fun getPosition(i: Int): MetaContainer {
        val (line, char) = newlineLocations.indexOfLast { it < i }.let {
            if (it == -1) {
                1 to i + 1
            } else {
                val line = it + 2
                val char = i - newlineLocations[it]
                line to char
            }
        }
        return metaContainerOf(
            "pos" to (line to char),
            "index" to i
        )
    }

    val tokens = mutableListOf<IonTextToken>()
    var i = 0

    var iterations = 0 // Temporary to prevent infinite loops from unhandled edge cases

    while (i < text.length) {

        if (iterations > text.length * 2) TODO("Need to handle case: text[i]='${text[i]}'")
        iterations++

        when {
            // Whitespace
            text[i].isWhitespace() -> {
                val range = text.collectUntil(i) { !this[it].isWhitespace() }
                tokens.add(SimpleToken(text[range], TokenType.WhiteSpace, getPosition(i)))
                i = range.last + 1
            }
            // Comments
            text[2 from i] == "//" -> {
                val range = text.collectUntil(start = i) { text[it] == '\n' }
                tokens.add(SimpleToken(text[range], TokenType.Comment, getPosition(i)))
                i = range.last + 1
            }
            text[2 from i] == "/*" -> {
                val range = text.collectUntil(start = i) { text[2 from it] == "*/" }
                tokens.add(SimpleToken(text[range] + "*/", TokenType.Comment, getPosition(i)))
                i = range.last + 1 + 2
            }
            // Lobs

            text[2 from i] == "{{" -> {
                tokens.add(SimpleToken(text[2 from i], TokenType.Delimiter, getPosition(i)))
                i += 2

                while (i < text.length && text[2 from i] != "}}") {
                    when {
                        text[i].isWhitespace() -> {
                            val range = text.collectUntil(i) { !get(i).isWhitespace() }
                            tokens.add(SimpleToken(text[range], TokenType.WhiteSpace, getPosition(i)))
                            i = range.last + 1
                        }
                        // TODO: Long strings
                        text[i] == '"' -> {
                            val range = text.collectUntil(i + 1) { text[it-1] != '\\' && text[it] == '"' }
                            tokens.add(SimpleToken(text[i..range.last+1], TokenType.PartialValue, getPosition(i)))
                            i = range.last + 2
                        }
                        else -> {
                            val range = text.collectUntil(i) { text[it].isWhitespace() || text[it] == '}' }
                            tokens.add(SimpleToken(text[range], TokenType.PartialValue, getPosition(i)))
                            i = range.last + 1
                        }
                    }
                }
                tokens.add(SimpleToken(text[2 from i], TokenType.Delimiter, getPosition(i)))
                i += 2
            }

            // Nulls
            text[i until i + 4] == "null" -> {
                when (text[i + 4]) {
                    '.' -> {
                        // Typed null
                        val range = text.collectUntil(i + 5) { text[it] !in unquotedSymbolCharacters }
                        tokens.add(SimpleToken(text[i..range.last], TokenType.PartialValue, getPosition(i)))
                        i = range.last + 1
                    }
                    !in unquotedSymbolCharacters -> {
                        // Unadorned null
                        tokens.add(SimpleToken(text[i until i + 4], TokenType.PartialValue, getPosition(i)))
                        i += 4
                    }
                    // it's actually a symbol
                    else -> {
                        val range = text.collectUntil(i) { text[it] !in unquotedSymbolCharacters }
                        tokens.add(SimpleToken(text[range], TokenType.PartialValue, getPosition(i)))
                        i = range.last + 1
                    }
                }
            }
            // Long Strings
            text[3 from i] == "'''" -> {
                val range = text.collectUntil(i + 3) { text[it-1] != '\\' && text[3 from it] == "'''" }
                tokens.add(SimpleToken(text[i..range.last+3], TokenType.PartialValue, getPosition(i)))
                i = range.last + 1 + 3
            }
            // Random Delimiters
            text[2 from i] in DELIMITERS -> {
                tokens.add(SimpleToken(text[2 from i], TokenType.Delimiter, getPosition(i)))
                i += 2
            }
            // Numbers & Timestamps
            text[i] in "0123456789"
                || text[i] == '-' && text[i + 1] in "0123456789" -> {
                val end = text.indexOfCharOrEnd(i) { it in numericStopCharacters }
                tokens.add(SimpleToken(text[i until end], TokenType.PartialValue, getPosition(i)))
                i = end
            }
            // Regular strings
            text[i] == '"'  -> {
                val range = text.collectUntil(i + 1) { text[it-1] != '\\' && text[it] == '"' }
                tokens.add(SimpleToken(text[i..range.last+1], TokenType.PartialValue, getPosition(i)))
                i = range.last + 2
            }
            // Quoted symbols
            text[i] == '\''  -> {
                val range = text.collectUntil(i + 1) { text[it-1] != '\\' && text[it] == '\'' }
                tokens.add(SimpleToken(text[i..range.last+1], TokenType.PartialValue, getPosition(i)))
                i = range.last + 2
            }
            // Delimiters
            text[1 from i] in DELIMITERS -> {
                tokens.add(SimpleToken(text[1 from i], TokenType.Delimiter, getPosition(i)))
                i += 1
            }
            // Unquoted Symbols & Booleans
            text[i].isLetter()
                || text[i] == '_'
                || text[i] == '$' -> {
                val range = text.collectUntil(i) { text[it] !in unquotedSymbolCharacters }
                tokens.add(SimpleToken(text[range], TokenType.PartialValue, getPosition(i)))
                i = range.last + 1
            }
            text[i] in symbolOperatorCharacters -> {
                val range = text.collectUntil(i) { text[it] !in symbolOperatorCharacters }
                tokens.add(SimpleToken(text[range], TokenType.PartialValue, getPosition(i)))
                i = range.last + 1
            }
        }



    }
    return tokens.toPersistentList()
}

fun String.collectUntil(start: Int, isEnd: String.(Int) -> Boolean): IntRange {
    var i = start
    while (i < length && !isEnd(i)) {
        i++
    }
    return start until i
}
fun String.indexOfOrEnd(startIndex: Int, predicate: (String, Int) -> Boolean): Int {
    var i = startIndex
    while (i < length && !predicate(this, i)) i++
    return i
}
fun String.indexOfCharOrEnd(startIndex: Int, predicate: (Char) -> Boolean): Int {
    var i = startIndex
    while (i < length && !predicate(this[i])) i++
    return i
}

// TODO
class IonTextTokenDelegatingImpl(
    private val contentSource: String,
    private val contentRange: IntRange,
    override val type: TokenType,
    override val metas: MetaContainer = emptyMetaContainer()
): IonTextToken() {
    override val content: CharSequence
        get() = contentSource.slice(contentRange)
}
