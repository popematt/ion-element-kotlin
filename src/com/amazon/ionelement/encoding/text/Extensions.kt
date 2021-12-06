package com.amazon.ionelement.encoding.text

sealed class SplitResult<E> {
    data class Delimiter<E>(val element: E): SplitResult<E>()
    data class Group<E>(val group: List<E>): SplitResult<E>()
}

fun <E> List<E>.splitOnElements(isDelimiter: (E) -> Boolean): List<SplitResult<E>> {
    if (isEmpty()) return emptyList()

    val groupings = mutableListOf<SplitResult<E>>()
    var groupStartIndex = 0
    forEachIndexed { index, e ->
        if (isDelimiter(e)) {
            groupings.add(SplitResult.Group(subList(groupStartIndex, index)))
            groupings.add(SplitResult.Delimiter(e))
            groupStartIndex = index + 1
        }
    }
    groupings.add(SplitResult.Group(subList(groupStartIndex, size)))
    return groupings.toList()
}

fun <E> List<E>.splitBetweenElements(isBoundary: (a: E, b: E) -> Boolean): List<List<E>> {
    val iterator = iterator().withIndex()
    val groupings = mutableListOf<List<E>>()
    if (!iterator.hasNext()) return emptyList()
    var (groupStartIndex, current) = iterator.next()
    while (iterator.hasNext()) {
        val (index, next) = iterator.next()
        if (isBoundary(current, next)) {
            groupings.add(subList(groupStartIndex, index))
            groupStartIndex = index
        }
        current = next
    }

    groupings.add(subList(groupStartIndex, size))
    return groupings.toList()
}

fun CharSequence.trimQuotes(quoteString: String): CharSequence {
    return if (startsWith(quoteString) && endsWith(quoteString))
        substring(quoteString.length, length - quoteString.length)
    else
        this
}
