// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.ionelement.wrapper

import com.amazon.ion.IonValue
import com.amazon.ion.IonWriter
import com.amazon.ionelement.api.*
import com.amazon.ionelement.impl.*

/**
 * Base class for all IonElement implementations that wrap an IonValue.
 */
internal abstract class AnyValueWrapper<T : IonValue>(internal val delegate: T) : AnyElementBase(), IonValueWrapper {
    init { delegate.makeReadOnly() }

    final override fun unwrap(): IonValue = delegate

    final override val metas: MetaContainer
        get() = emptyMetaContainer()

    final override val annotations: List<String>
        get() = handleIonException { delegate.typeAnnotations.toList() }

    final override fun writeTo(writer: IonWriter) { delegate.writeTo(writer) }

    final override fun writeContentTo(writer: IonWriter) = TODO("Not implemented because AnyValueWrapper has a custom writeTo implementation.")

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other is IonValueWrapper) return this.unwrap() == other.unwrap()
        if (other !is IonElement) return false
        return other.asAnyElement().isEquivalentTo(delegate)
    }

    override fun hashCode(): Int = hashIonValue(delegate)
}
