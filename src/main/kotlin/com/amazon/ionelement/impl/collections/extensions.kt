package com.amazon.ionelement.impl.collections

import com.amazon.ionelement.api.*
import kotlinx.collections.immutable.adapters.ImmutableListAdapter
import kotlinx.collections.immutable.adapters.ImmutableMapAdapter

typealias ImmutableList<E> = kotlinx.collections.immutable.ImmutableList<E>
typealias ImmutableMap<K, V> = kotlinx.collections.immutable.ImmutableMap<K, V>

internal val EMPTY_IMMUTABLE_MAP = ImmutableMapAdapter<Any?, Nothing>(emptyMap())
internal val EMPTY_IMMUTABLE_LIST = ImmutableListAdapter(emptyList<Nothing>())

/**
 * Creates a [ImmutableMap] for [this] without making a defensive copy.
 * Only call this method if you are sure that [this] cannot leak anywhere it could be mutated.
 */
internal fun <K, V> Map<K, V>.toImmutableMapUnsafe(): ImmutableMap<K, V> {
    if (this is ImmutableMap) return this
    // Empty ImmutableMap can be safely cast to any `<K, V>` because it is empty.
    @Suppress("UNCHECKED_CAST")
    if (isEmpty()) return EMPTY_IMMUTABLE_MAP as ImmutableMap<K, V>
    return ImmutableMapAdapter(this)
}

/**
 * Creates a [ImmutableMap] for [this].
 * This function creates a defensive copy of [this] unless [this] is already a [ImmutableMap].
 */
internal fun <K, V> Map<K, V>.toImmutableMap(): ImmutableMap<K, V> {
    if (this is ImmutableMap) return this
    // Empty ImmutableMap can be safely cast to any `<K, V>` because it is empty.
    @Suppress("UNCHECKED_CAST")
    if (isEmpty()) return (EMPTY_IMMUTABLE_MAP as ImmutableMap<K, V>)
    return ImmutableMapAdapter(toMap())
}

/**
 * Creates an [ImmutableMetaContainer] ([ImmutableMap]) that holds [this] [IonLocation] instance.
 */
internal fun IonLocation.toMetaContainer(): ImmutableMap<String, Any> {
    return IonLocationBackedImmutableMap(this)
}

/**
 * Creates a [ImmutableList] for [this].
 * This function creates a defensive copy of [this] unless [this] is already a [ImmutableList].
 */
internal fun <E> Iterable<E>.toImmutableList(): ImmutableList<E> {
    if (this is ImmutableList<E>) return this
    val isEmpty = if (this is Collection<*>) {
        this.isEmpty()
    } else {
        !this.iterator().hasNext()
    }
    return if (isEmpty) EMPTY_IMMUTABLE_LIST else ImmutableListAdapter(this.toList())
}

/**
 * Creates a [ImmutableList] for [this].
 * This function creates a defensive copy of [this] unless [this] is already a [ImmutableList].
 */
internal fun <E> List<E>.toImmutableList(): ImmutableList<E> {
    if (this is ImmutableList<E>) return this
    if (isEmpty()) return EMPTY_IMMUTABLE_LIST
    return ImmutableListAdapter(this.toList())
}

/**
 * Creates a [ImmutableList] for [this] without making a defensive copy.
 * Only call this method if you are sure that [this] cannot leak anywhere it could be mutated.
 */
internal fun <E> List<E>.toImmutableListUnsafe(): ImmutableList<E> {
    if (this is ImmutableList<E>) return this
    if (isEmpty()) return EMPTY_IMMUTABLE_LIST
    return ImmutableListAdapter(this)
}

/**
 * Creates a [ImmutableList] for [this] without making a defensive copy.
 * Only call this method if you are sure that [this] cannot leak anywhere it could be mutated.
 */
internal fun <E> List<E>.toImmutableListUnsafeAllowModification(): ImmutableList<E> {
    if (this is ImmutableList<E>) return this
    return ImmutableListAdapter(this)
}

/**
 * Creates a [ImmutableList] for [this] without making a defensive copy.
 * Only call this method if you are sure that [this] cannot leak anywhere it could be mutated.
 */
internal fun <E> Array<E>.toImmutableListUnsafe(): ImmutableList<E> {
    if (isEmpty()) return EMPTY_IMMUTABLE_LIST
    // This wraps the array in an ArrayList and then in an ImmutableListAdapter. In theory, we could reduce the overhead
    // even further but using only a single wrapper layer, if we created such a thing.
    return ImmutableListAdapter(asList())
}


internal interface SometimesMutableList<E> {
    fun add(element: E)
    fun seal(): ImmutableList<E>
}

private class SometimesMutableListImpl<E> private constructor(private val impl: MutableList<E>) : SometimesMutableList<E>, ImmutableList<E>, List<E> by impl {
    constructor(): this(mutableListOf())

    private var isClosed = false

    override fun add(element: E) {
        if (isClosed) throw UnsupportedOperationException()
        impl.add(element)
    }

    override fun seal(): ImmutableList<E> {
        isClosed = true
        return this
    }

    override fun subList(fromIndex: Int, toIndex: Int): ImmutableList<E> = ImmutableListAdapter(impl.subList(fromIndex, toIndex))

    override fun equals(other: Any?): Boolean = impl.equals(other)
    override fun hashCode(): Int = impl.hashCode()
    override fun toString(): String = impl.toString()
}
