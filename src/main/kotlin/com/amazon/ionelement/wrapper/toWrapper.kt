// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.ionelement.wrapper

import com.amazon.ion.IntegerSize
import com.amazon.ion.IonBlob
import com.amazon.ion.IonBool
import com.amazon.ion.IonClob
import com.amazon.ion.IonDecimal
import com.amazon.ion.IonException
import com.amazon.ion.IonFloat
import com.amazon.ion.IonInt
import com.amazon.ion.IonList
import com.amazon.ion.IonSexp
import com.amazon.ion.IonString
import com.amazon.ion.IonStruct
import com.amazon.ion.IonSymbol
import com.amazon.ion.IonTimestamp
import com.amazon.ion.IonType
import com.amazon.ion.IonValue
import com.amazon.ionelement.api.*
import com.amazon.ionelement.impl.*
import com.amazon.ionelement.impl.collections.*

/**
 * Internal-only function that converts an [IonValue] to an [AnyElement] by (maybe) wrapping it
 * in an [IonValueWrapper].
 *
 * Currently, there are no wrappers for scalar types since the overhead is unlikely to be worthwhile.
 * However, the `IonValueWrapper` is not exposed directly to users, so it would be possible to add
 * scalar wrappers if they are deemed worthwhile.
 */
internal fun IonValue.toWrapper(): AnyElement = handleIonException {
    val annotations = ArrayList<String>(typeAnnotations.size)
        .apply { addAll(typeAnnotations) }
        .toImmutableListUnsafe()

    if (isNullValue) {
        NullElementImpl(type.toElementType(), typeAnnotations.toList().toImmutableListUnsafe(), EMPTY_METAS)
    } else when (type) {
        IonType.BOOL -> ionBool((this as IonBool).booleanValue(), annotations)
        IonType.INT -> when ((this as IonInt).integerSize!!) {
            IntegerSize.BIG_INTEGER -> {
                val bigIntValue = bigIntegerValue()
                // Ion java's IonReader appears to determine integerSize based on number of bits,
                // not on the actual value, which means if we have a padded int that is > 63 bits,
                // but whose value only uses <= 63 bits then integerSize is still BIG_INTEGER.
                // Compensate for that here...
                if (bigIntValue !in RANGE_OF_LONG)
                    ionInt(bigIntValue, annotations)
                else {
                    ionInt(longValue(), annotations)
                }
            }
            IntegerSize.LONG, IntegerSize.INT -> ionInt(this.longValue(), annotations)
        }
        IonType.FLOAT -> ionFloat((this as IonFloat).doubleValue(), annotations)
        IonType.DECIMAL -> ionDecimal((this as IonDecimal).decimalValue(), annotations)
        IonType.TIMESTAMP -> ionTimestamp((this as IonTimestamp).timestampValue(), annotations)
        IonType.STRING -> ionString((this as IonString).stringValue(), annotations)
        IonType.SYMBOL -> ionSymbol((this as IonSymbol).stringValue(), annotations)
        IonType.CLOB -> ionClob((this as IonClob).bytes, annotations)
        IonType.BLOB -> ionBlob((this as IonBlob).bytes, annotations)
        IonType.LIST -> ListValueWrapper(this as IonList)
        IonType.SEXP -> SexpValueWrapper(this as IonSexp)
        IonType.STRUCT -> StructValueWrapper(this as IonStruct)
        IonType.DATAGRAM -> throw IonElementWrapperException(
            this,
            "IonDatagram cannot be converted to IonElement. Convert the values of the datagram instead."
        )
        IonType.NULL, null -> TODO("Unreachable")
    }.asAnyElement()
}

internal fun <T> IonValueWrapper.handleIonException(block: () -> T): T = unwrap().handleIonException { block() }

internal fun <T> IonValue.handleIonException(block: IonValue.() -> T): T {
    try {
        return block()
    } catch (e: IonException) {
        throw IonElementWrapperException(
            blame = this,
            description = "IonException occurred, likely due to an IonValue that is unrepresentable as an IonElement (see cause)",
            cause = e
        )
    }
}
