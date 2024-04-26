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

@file:JvmName("IonUtils")
package com.amazon.ionelement.api

import com.amazon.ion.IonValue
import com.amazon.ion.ValueFactory
import com.amazon.ionelement.wrapper.*
import com.amazon.ionelement.wrapper.IonValueWrapper

/**
 * Bridge function that converts from an immutable [IonElement] to a mutable [IonValue].
 *
 * New code that doesn't need to integrate with existing uses of the mutable DOM should not use this.
 *
 * @param factory A [ValueFactory] to use to create the new [IonValue] instances.  Note that any
 * [com.amazon.ion.IonSystem] instance maybe be used here, in addition to other implementations of [ValueFactory].
 */
public fun IonElement.toIonValue(factory: ValueFactory): IonValue {
    if (this is IonValueWrapper) return factory.clone(unwrap())

    // We still have to use IonSystem under the covers for this to get an IonWriter that writes to a dummy list.
    val dummyList = factory.newList()
    dummyList.system.newWriter(dummyList).use { writer ->
        this.writeTo(writer)
    }
    // .removeAt(0) below detaches the `IonValue` that was written above so that it may be added to other
    // IonContainer instances without needing to be `IonValue.cloned()`'d.
    return dummyList.removeAt(0)
}

/**
 * Bridge function that converts from an immutable [IonElement] to a read-only [IonValue].
 * This is not guaranteed to use the given valueFactory.
 *
 * New code that doesn't need to integrate with existing uses of the mutable DOM should not use this.
 *
 * @param factory A [ValueFactory] to use to create the new [IonValue] instances.  Note that any
 * [com.amazon.ion.IonSystem] instance maybe be used here, in addition to other implementations of [ValueFactory].
 */
public fun IonElement.toReadOnlyIonValue(factory: ValueFactory): IonValue {
    if (this is IonValueWrapper) return unwrap()

    // We still have to use IonSystem under the covers for this to get an IonWriter that writes to a dummy list.
    val dummyList = factory.newList()
    dummyList.system.newWriter(dummyList).use { writer ->
        this.writeTo(writer)
    }
    // .removeAt(0) below detaches the `IonValue` that was written above so that it may be added to other
    // IonContainer instances without needing to be `IonValue.cloned()`'d.
    return dummyList.removeAt(0).apply(IonValue::makeReadOnly)
}

/**
 * Bridge function that converts from the mutable [IonValue] to an [AnyElement].
 *
 * New code that does not need to integrate with uses of the mutable DOM should not use this.
 *
 * This will fail for IonDatagram if the IonDatagram does not contain exactly one user value.
 */
public fun IonValue.toIonElement(): AnyElement =
    this.system.newReader(this).use { reader ->
        createIonElementLoader().loadSingleElement(reader)
    }

/**
 * Bridge function that converts from a _read-only_ [IonValue] to an [AnyElement].
 *
 * If you need to keep a mutable copy of your [IonValue], use [toIonElement] instead.
 *
 * New code that does not need to integrate with uses of the mutable DOM should not use this.
 */
public fun IonValue.wrapIntoIonElement(): AnyElement {
    // Throws if the value is not read-only
    if (!isReadOnly) throw IonElementException(location = null, "Cannot wrap an IonValue instance that is not read-only.")
    return toWrapper()
}

/**
 * Bridge function that converts from the mutable [IonValue] to an [AnyElement].
 *
 * This does not check whether the given [IonValue] is read-only—it calls [IonValue.makeReadOnly]
 * unconditionally in order to ensure that the wrapped value can fulfill the immutability requirement
 * of the [IonElement] interface.
 * This will ensure a value is read-only in order to protect its own invariants, BUT BEWARE,
 * making something read-only is a side effect that could have unintended consequences elsewhere.
 *
 * If you need to keep a mutable copy of your [IonValue], use [toIonElement] instead.
 *
 * New code that does not need to integrate with uses of the mutable DOM should not use this.
 */
public fun IonValue.wrapUncheckedIntoIonElement(): AnyElement = this.apply { makeReadOnly() }.toWrapper()

/** Throws an [IonElementException], including the [IonLocation] (if available). */
internal fun constraintError(blame: IonElement, description: String): Nothing {
    throw IonElementConstraintException(blame.asAnyElement(), description)
}
