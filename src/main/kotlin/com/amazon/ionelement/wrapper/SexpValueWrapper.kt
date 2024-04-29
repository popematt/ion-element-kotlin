// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.ionelement.wrapper

import com.amazon.ion.IonSexp
import com.amazon.ionelement.api.*
import com.amazon.ionelement.impl.*
import com.amazon.ionelement.impl.SexpElementImpl
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap

internal class SexpValueWrapper(delegate: IonSexp) : AnyValueWrapper<IonSexp>(delegate), SexpElement, UnionOfSexpAndAnyElement {
    override val type: ElementType get() = ElementType.SEXP

    override val size: Int get() = delegate.size
    override val containerValues: List<AnyElement> get() = values
    override val seqValues: List<AnyElement> get() = values
    override val sexpValues: List<AnyElement> get() = values

    private lateinit var valuesBackingField: PersistentList<AnyElement>

    override val values: List<AnyElement>
        get() {
            if (!this::valuesBackingField.isInitialized) {
                valuesBackingField = delegate.mapTo(persistentListOf<AnyElement>().builder()) { it.toIonElement() }.build()
            }
            return valuesBackingField
        }

    override fun copy(annotations: List<String>, metas: MetaContainer): SexpElementImpl =
        SexpElementImpl(values as PersistentList<AnyElement>, annotations.toPersistentList(), metas.toPersistentMap())
}
