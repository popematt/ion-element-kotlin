// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
@file:JvmName("Equivalence")
package com.amazon.ionelement.api

import com.amazon.ion.Decimal
import com.amazon.ion.IntegerSize
import com.amazon.ion.IonBool
import com.amazon.ion.IonDecimal
import com.amazon.ion.IonFloat
import com.amazon.ion.IonInt
import com.amazon.ion.IonLob
import com.amazon.ion.IonSequence
import com.amazon.ion.IonString
import com.amazon.ion.IonStruct
import com.amazon.ion.IonSymbol
import com.amazon.ion.IonTimestamp
import com.amazon.ion.IonType
import com.amazon.ion.IonValue
import com.amazon.ionelement.api.ElementType.*

/**
 * Checks if two [IonElement]s are equal.
 *
 * This function is normative for equality of [IonElement]s.
 * All [IonElement] implementations must override [Any.equals] in a way that is equivalent to calling this function.
 */
public fun areElementsEqual(left: IonElement, right: IonElement): Boolean {
    return left.asAnyElement().isEquivalentTo(right.asAnyElement())
}

/**
 * Checks if an [IonElement] and an [IonValue] are equivalent.
 */
internal fun AnyElement.isEquivalentTo(other: IonValue): Boolean {
    val thisType = this.type.toIonType()
    if (thisType != other.type) return false

    val otherAnnotations: List<String?> = other.typeAnnotationSymbols.map { it.text }
    if (annotations != otherAnnotations) return false

    return if (isNull) {
        // Already verified that they are the same type
        other.isNullValue
    } else if (other.isNullValue) {
        false
    } else
    // Matching an enum rather than a type allows the Kotlin compiler
    // to use a table switch instead of a chain of if/else comparisons.
        when (thisType) {
            IonType.BOOL -> booleanValue == (other as IonBool).booleanValue()
            IonType.INT -> {
                other as IonInt
                if (this.integerSize == IntElementSize.LONG && other.integerSize != IntegerSize.BIG_INTEGER) {
                    this.longValue == other.longValue()
                } else {
                    this.bigIntegerValue == other.bigIntegerValue()
                }
            }
            // compareTo() distinguishes between 0.0 and -0.0 while `==` operator does not.
            IonType.FLOAT -> doubleValue.compareTo((other as IonFloat).doubleValue()) == 0
            // `==` considers `0d0` and `-0d0` to be equivalent.  `Decimal.equals` does not.
            IonType.DECIMAL -> Decimal.equals(decimalValue, (other as IonDecimal).decimalValue())
            IonType.TIMESTAMP -> timestampValue == (other as IonTimestamp).timestampValue()
            IonType.STRING -> stringValue == (other as IonString).stringValue()
            IonType.SYMBOL -> symbolValue == (other as IonSymbol).symbolValue().text

            IonType.BLOB,
            IonType.CLOB -> {
                this.bytesValue.contentEquals((other as IonLob).bytes)
            }
            IonType.SEXP,
            IonType.LIST -> {
                other as IonSequence
                seqValues.size == other.size && seqValues.indices.all { i -> seqValues[i].isEquivalentTo(other[i]) }
            }
            IonType.STRUCT -> {
                other as IonStruct
                val thisFields = this.structFields
                if (thisFields.size != other.size()) return false
                val thisFieldsGroups = this.structFields.groupBy(StructField::name, StructField::value)
                val otherFieldGroups: Map<String?, List<IonValue>> = other.groupBy { it.fieldNameSymbol.text }
                // Do they have the same number of field names?
                if (thisFieldsGroups.size != otherFieldGroups.size) return false
                thisFieldsGroups.all { (fieldName, valuesGroup) ->
                    val thisSubGroup: Map<AnyElement, Int> = valuesGroup.groupingBy { it }.eachCount()
                    val otherGroup = otherFieldGroups[fieldName]
                    thisSubGroup.all { (value, count) -> count == otherGroup?.count { value.isEquivalentTo(it) } }
                }
            }
            IonType.NULL,
            IonType.DATAGRAM -> TODO("Unreachable")
        }
}

/**
 * Checks if two [StructField]s are equal.
 *
 * This function is normative for equality of [StructField]s.
 * All [StructField] implementations must override [Any.equals] in a way that is equivalent to calling this function.
 */
public fun areFieldsEqual(left: StructField, right: StructField): Boolean = left.name == right.name && left.value == right.value

/**
 * Checks if this [AnyElement] is equal to some other object.
 */
internal fun AnyElement.isEquivalentTo(other: Any?): Boolean {
    if (this === other) return true
    if (other !is AnyElement) return false
    return this.isEquivalentTo(other)
}

/**
 * Internal only function that is equivalent to [areElementsEqual]
 */
private fun AnyElement.isEquivalentTo(other: AnyElement): Boolean {
    if (this === other) return true
    val thisType = this.type
    if (thisType != other.type) return false
    if (annotations != other.annotations) return false
    // Metas intentionally not included here.

    return if (isNull) {
        // Already verified that they are the same type
        other.isNull
    } else if (other.isNull) {
        false
    } else
    // Matching an enum rather than a type allows the Kotlin compiler
    // to use a table switch instead of a chain of if/else comparisons.
        when (thisType) {
            BOOL -> booleanValue == other.booleanValue
            INT -> when {
                integerSize != other.integerSize -> false
                integerSize == IntElementSize.LONG -> longValue == other.longValue
                else -> bigIntegerValue == other.bigIntegerValue
            }
            // compareTo() distinguishes between 0.0 and -0.0 while `==` operator does not.
            FLOAT -> doubleValue.compareTo(other.doubleValue) == 0
            // `==` considers `0d0` and `-0d0` to be equivalent.  `Decimal.equals` does not.
            DECIMAL -> Decimal.equals(decimalValue, other.decimalValue)
            TIMESTAMP -> timestampValue == other.timestampValue
            STRING -> stringValue == other.stringValue
            SYMBOL -> symbolValue == other.symbolValue
            BLOB -> blobValue == other.blobValue
            CLOB -> clobValue == other.clobValue
            LIST -> listValues == other.listValues
            SEXP -> sexpValues == other.sexpValues
            STRUCT -> {
                val thisFields = this.structFields
                val otherFields = other.structFields
                when {
                    thisFields === otherFields -> true
                    thisFields.size != otherFields.size -> false
                    // We've tried the inexpensive checks, now do a deep comparison
                    else -> thisFields.groupingBy { it }.eachCount() == otherFields.groupingBy { it }.eachCount()
                }
            }
            NULL -> TODO("Unreachable")
        }
}

/**
 * Calculates the hash code of an [IonElement].
 *
 * Implementations of [IonElement] MAY NOT calculate their own hash codes. However, they MAY store the result of this
 * function as a private field. The result of this function may change from one release to another—do not put this
 * value in any persistent storage.
 */
public fun hashElement(ionElement: IonElement): Int {
    val element = ionElement.asAnyElement()
    val typeAndValueHashCode = if (element.isNull) {
        element.type.hashCode()
    } else {
        // Matching an enum rather than a type allows the Kotlin compiler
        // to use a tableswitch instead of a chain of if/else comparisons.
        val valueHashCode = when (element.type) {
            BOOL -> element.booleanValue.hashCode()
            INT -> when (element.integerSize) {
                IntElementSize.LONG -> element.longValue.hashCode()
                IntElementSize.BIG_INTEGER -> element.bigIntegerValue.hashCode()
            }
            // Adding compareTo(0.0) causes 0e0 to have a different hash code than -0e0
            FLOAT -> element.doubleValue.compareTo(0.0).hashCode() * 31 + element.doubleValue.hashCode()
            DECIMAL -> element.decimalValue.isNegativeZero.hashCode() * 31 + element.decimalValue.hashCode()
            TIMESTAMP -> element.timestampValue.hashCode()
            STRING -> element.textValue.hashCode()
            SYMBOL -> element.textValue.hashCode()
            BLOB -> element.bytesValue.hashCode()
            CLOB -> element.bytesValue.hashCode()
            LIST -> element.listValues.hashCode()
            SEXP -> element.sexpValues.hashCode()
            STRUCT -> element.structFields.map { it.hashCode() }.sorted().hashCode()
            NULL -> TODO("Unreachable")
        }
        element.type.hashCode() * 31 + valueHashCode
    }
    return typeAndValueHashCode * 31 + element.annotations.hashCode()
}

/**
 * Calculates a hashcode for an [IonValue] using the same algorithm as [hashElement].
 *
 * SymbolTokens with unknown text have a hashcode value of 0 (see [com.amazon.ion.impl.SymbolTokenImpl]),
 * which is the same as an empty string. This is still consistent with [AnyElement.isEquivalentTo] because
 * SymbolTokens with unknown text are generally unrepresentable in [IonElement].
 *
 * DO NOT MAKE THIS PUBLIC. Why???
 */
internal fun hashIonValue(ionValue: IonValue): Int {
    val typeAndValueHashCode = if (ionValue.isNullValue) {
        ionValue.type.toElementType().hashCode()
    } else {
        // Matching an enum rather than a type allows the Kotlin compiler
        // to use a tableswitch instead of a chain of if/else comparisons.
        val valueHashCode = when (ionValue.type!!) {
            IonType.BOOL -> (ionValue as IonBool).booleanValue().hashCode()
            IonType.INT -> {
                ionValue as IonInt
                if (ionValue.integerSize == IntegerSize.BIG_INTEGER) {
                    ionValue.bigIntegerValue().hashCode()
                } else {
                    ionValue.longValue().hashCode()
                }
            }
            // Adding compareTo(0.0) causes 0e0 to have a different hash code than -0e0
            IonType.FLOAT -> (ionValue as IonFloat).doubleValue().let { it.compareTo(0.0).hashCode() * 31 + it.hashCode() }
            IonType.DECIMAL -> (ionValue as IonDecimal).decimalValue().let { it.isNegativeZero.hashCode() * 31 + it.hashCode() }
            IonType.TIMESTAMP -> (ionValue as IonTimestamp).timestampValue().hashCode()
            IonType.STRING -> (ionValue as IonString).stringValue().hashCode()
            IonType.SYMBOL -> (ionValue as IonSymbol).symbolValue().hashCode()
            IonType.BLOB,
            IonType.CLOB -> (ionValue as IonLob).bytes!!.contentHashCode()
            IonType.LIST,
            IonType.SEXP -> (ionValue as IonSequence).map { hashIonValue(it) }.hashCode()
            IonType.STRUCT -> {
                ionValue as IonStruct
                ionValue.map { hashIonValueField(it) }.sorted().hashCode()
            }
            IonType.DATAGRAM -> throw IllegalArgumentException("This function does not support hashing an IonDatagram")
            IonType.NULL -> TODO("Unreachable")
        }
        ionValue.type.toElementType().hashCode() * 31 + valueHashCode
    }
    return typeAndValueHashCode * 31 + ionValue.typeAnnotationSymbols.toList().hashCode()
}

/**
 * Calculates the hash code of a [StructField].
 *
 * Implementations of [StructField] MAY NOT calculate their own hash codes. However, they MAY store the result of this
 * function as a private field. The result of this function may change from one release to another—do not put this
 * value in any persistent storage.
 */
public fun hashField(structField: StructField): Int {
    return structField.name.hashCode() * 31 + structField.value.hashCode()
}

private fun hashIonValueField(ionValue: IonValue): Int {
    return ionValue.fieldNameSymbol.hashCode() * 31 + hashIonValue(ionValue)
}
