/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package com.amazon.ionelement.impl

import com.amazon.ionelement.api.*
import com.amazon.ionelement.api.ImmutableMetaContainer
import com.amazon.ionelement.impl.collections.*

internal class SexpElementImpl(
    values: ImmutableList<AnyElement>,
    override val annotations: ImmutableList<String>,
    override val metas: ImmutableMetaContainer
) : SeqElementBase(values), SexpElement, UnionOfSexpAndAnyElement {
    override val type: ElementType get() = ElementType.SEXP

    override val sexpValues: List<AnyElement> get() = seqValues

    override fun copy(annotations: List<String>, metas: MetaContainer): SexpElementImpl =
        SexpElementImpl(values, annotations.toImmutableList(), metas.toImmutableMap())

    override fun withAnnotations(vararg additionalAnnotations: String): SexpElementImpl = _withAnnotations(*additionalAnnotations)
    override fun withAnnotations(additionalAnnotations: Iterable<String>): SexpElementImpl = _withAnnotations(additionalAnnotations)
    override fun withoutAnnotations(): SexpElementImpl = _withoutAnnotations()
    override fun withMetas(additionalMetas: MetaContainer): SexpElementImpl = _withMetas(additionalMetas)
    override fun withMeta(key: String, value: Any): SexpElementImpl = _withMeta(key, value)
    override fun withoutMetas(): SexpElementImpl = _withoutMetas()

    override fun equals(other: Any?): Boolean = isEquivalentTo(other)
    override fun hashCode(): Int = hashElement(this)
}
