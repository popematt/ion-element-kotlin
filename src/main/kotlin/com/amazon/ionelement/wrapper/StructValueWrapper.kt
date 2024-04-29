// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.ionelement.wrapper

import com.amazon.ion.IonStruct
import com.amazon.ionelement.api.*
import com.amazon.ionelement.impl.MutableStructFieldsImpl
import com.amazon.ionelement.impl.StructElementImpl
import com.amazon.ionelement.impl._withAnnotations
import com.amazon.ionelement.impl._withMeta
import com.amazon.ionelement.impl._withMetas
import com.amazon.ionelement.impl._withoutAnnotations
import java.util.function.Consumer
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap

internal class StructValueWrapper(delegate: IonStruct) : AnyValueWrapper<IonStruct>(delegate), StructElement {
    override val type: ElementType get() = ElementType.STRUCT

    override val size: Int get() = delegate.size()

    override val containerValues: Collection<AnyElement> get() = values
    override val structFields: Collection<StructField> get() = fields

    private lateinit var valuesBackingField: PersistentList<AnyElement>
    override val values: Collection<AnyElement>
        get() {
            if (!this::valuesBackingField.isInitialized) {
                // Unchecked cast is safe because `fieldsBackingField` is a list.
                valuesBackingField = (fields as List<StructField>)
                    .mapTo(persistentListOf<AnyElement>().builder()) { it.value }.build()
            }
            return valuesBackingField
        }

    private lateinit var fieldsBackingField: PersistentList<StructField>
    override val fields: Collection<StructField>
        get() {
            if (!this::fieldsBackingField.isInitialized) {
                val listBuilder = persistentListOf<StructField>().builder()
                delegate.mapTo(listBuilder) { handleIonException { field(it.fieldNameSymbol.assumeText(), it.toWrapper()) } }
                fieldsBackingField = listBuilder.build()
            }
            return fieldsBackingField
        }

    private lateinit var fieldsByNameBackingField: PersistentMap<String, PersistentList<AnyElement>>

    /** Lazily calculated map of field names and lists of their values. */
    private val fieldsByName: Map<String, List<AnyElement>>
        get() {
            if (!this::fieldsByNameBackingField.isInitialized) {
                fieldsByNameBackingField = fields
                    .groupBy { it.name }
                    .map { structFieldGroup -> structFieldGroup.key to structFieldGroup.value.map { it.value }.toPersistentList() }
                    .toMap().toPersistentMap()
            }
            return fieldsByNameBackingField
        }

    override fun mutableFields(): MutableStructFields {
        val internalMap = mutableMapOf<String, MutableList<StructField>>()
        return MutableStructFieldsImpl(
            fieldsByName.mapValuesTo(internalMap) { (name, values) ->
                values.map { field(name, it) }.toMutableList()
            }
        )
    }

    override fun update(mutator: MutableStructFields.() -> Unit): StructElement {
        val mutableFields = mutableFields()
        mutableFields.apply(mutator)
        return ionStructOf(mutableFields, annotations, metas)
    }

    override fun update(mutator: Consumer<MutableStructFields>): StructElement {
        val mutableFields = mutableFields()
        mutator.accept(mutableFields)
        return ionStructOf(mutableFields, annotations, metas)
    }

    override fun get(fieldName: String): AnyElement =
        fieldsByName[fieldName]?.firstOrNull() ?: constraintError(this, "Required struct field '$fieldName' missing")

    override fun getOptional(fieldName: String): AnyElement? =
        fieldsByName[fieldName]?.firstOrNull()

    override fun getAll(fieldName: String): Iterable<AnyElement> = fieldsByName[fieldName] ?: emptyList()

    override fun containsField(fieldName: String): Boolean = delegate.containsKey(fieldName)

    override fun copy(annotations: List<String>, metas: MetaContainer): StructElementImpl =
        StructElementImpl(fields as PersistentList<StructField>, annotations.toPersistentList(), metas.toPersistentMap())

    override fun withAnnotations(vararg additionalAnnotations: String): StructElementImpl = _withAnnotations(*additionalAnnotations)
    override fun withAnnotations(additionalAnnotations: Iterable<String>): StructElementImpl = _withAnnotations(additionalAnnotations)
    override fun withoutAnnotations(): StructElementImpl = _withoutAnnotations()
    override fun withMetas(additionalMetas: MetaContainer): StructElementImpl = _withMetas(additionalMetas)
    override fun withMeta(key: String, value: Any): StructElementImpl = _withMeta(key, value)
    override fun withoutMetas(): StructValueWrapper = this

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StructElement) return false

        if (other is StructValueWrapper && delegate == other.delegate) return true

        if (annotations != other.annotations) return false

        // We might avoid materializing fields by checking fields.size first
        if (this.size != other.size) return false

        // We might avoid potentially expensive checks if the `fields` are the same instance, which
        // could occur if the only difference between the two StructElements is the metas.
        val thisFields = this.fields
        val otherFields = other.fields
        if (thisFields === otherFields) return true
        if (thisFields.size != otherFields.size) return false

        // This is potentially expensive, but so is a deep comparison, and at least hashcode can be cached.
        if (this.hashCode() != other.hashCode()) return false

        // We've tried all the inexpensive checks. Perform a deep comparison.
        return thisFields.groupingBy { it }.eachCount() == otherFields.groupingBy { it }.eachCount()
    }

    // Note that we are not using `by lazy` here because it requires 2 additional allocations and
    // has been demonstrated to significantly increase memory consumption!
    private var cachedHashCode: Int? = null
    override fun hashCode(): Int {
        if (this.cachedHashCode == null) {
            cachedHashCode = super.hashCode()
        }
        return this.cachedHashCode!!
    }
}
