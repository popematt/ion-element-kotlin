// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.ionelement.wrapper

import com.amazon.ion.IonList
import com.amazon.ionelement.api.AnyElement
import com.amazon.ionelement.api.ElementType
import com.amazon.ionelement.api.ListElement
import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.toIonElement
import com.amazon.ionelement.impl.ListElementImpl
import com.amazon.ionelement.impl._withAnnotations
import com.amazon.ionelement.impl._withMeta
import com.amazon.ionelement.impl._withMetas
import com.amazon.ionelement.impl._withoutAnnotations
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap

internal class ListValueWrapper(delegate: IonList) : AnyValueWrapper<IonList>(delegate), ListElement {
    override val type: ElementType get() = ElementType.LIST

    override val size: Int get() = delegate.size
    override val containerValues: List<AnyElement> get() = values
    override val seqValues: List<AnyElement> get() = values
    override val listValues: List<AnyElement> get() = values

    private lateinit var valuesBackingField: PersistentList<AnyElement>

    override val values: List<AnyElement>
        get() {
            if (!this::valuesBackingField.isInitialized) {
                valuesBackingField = delegate.mapTo(persistentListOf<AnyElement>().builder()) { it.toIonElement() }.build()
            }
            return valuesBackingField
        }

    override fun copy(annotations: List<String>, metas: MetaContainer): ListElementImpl =
        ListElementImpl(values as PersistentList<AnyElement>, annotations.toPersistentList(), metas.toPersistentMap())

    override fun withAnnotations(vararg additionalAnnotations: String): ListElementImpl = _withAnnotations(*additionalAnnotations)
    override fun withAnnotations(additionalAnnotations: Iterable<String>): ListElementImpl = _withAnnotations(additionalAnnotations)
    override fun withoutAnnotations(): ListElementImpl = _withoutAnnotations()
    override fun withMetas(additionalMetas: MetaContainer): ListElementImpl = _withMetas(additionalMetas)
    override fun withMeta(key: String, value: Any): ListElementImpl = _withMeta(key, value)
    override fun withoutMetas(): ListValueWrapper = this
}
