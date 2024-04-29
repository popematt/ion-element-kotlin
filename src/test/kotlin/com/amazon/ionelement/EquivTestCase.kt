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

package com.amazon.ionelement

import com.amazon.ionelement.api.*
import com.amazon.ionelement.impl.*
import com.amazon.ionelement.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assertions

data class EquivTestCase(val left: String, val right: String, val isEquiv: Boolean) {

    companion object {
        val loader = createIonElementLoader()
    }

    fun checkEquivalence() {

        // Note: metas are not relevant to equivalence tests.
        val leftElement = loader.loadSingleElement(left)
        val leftElementWithMetas = leftElement.withMeta("leftMeta", 1)
        assertEquals(leftElement, leftElementWithMetas)
        val leftProxy = leftElement.createProxy()
        assertEquals(leftElement, leftProxy)
        val leftWrapped = leftElement.toIonValue(ION).wrapUncheckedIntoIonElement()
        assertEquals(leftElement, leftWrapped)
        assertTrue(leftElement.isEquivalentTo(leftElement.toIonValue(ION)))

        val rightElement = loader.loadSingleElement(right)
        val rightElementWithMetas = rightElement.withMeta("rightMeta", 2)

        val rightWrapped = rightElement.toIonValue(ION).wrapUncheckedIntoIonElement()
        assertTrue(rightElement.isEquivalentTo(rightElement.toIonValue(ION)))
        assertEquals(rightElement.hashCode(), hashIonValue(rightElement.toIonValue(ION)))

        // Try using a proxy to make sure that equivalence is not tied to a particular implementation.
        val rightProxy = rightElement.createProxy()

        val leftElements = listOf(
            leftElement,
            leftProxy,
            leftElementWithMetas,
            leftWrapped,
        )

        val rightElements = listOf(
            rightElement,
            rightElementWithMetas,
            rightProxy,
            rightWrapped,
        )

        leftElements.forEach { l ->
            rightElements.forEach { r ->
                checkEquivalence(l, r)
            }
        }
    }

    private fun checkEquivalence(leftElement: AnyElement, rightElement: AnyElement) {

        // It seems unlikely that we should ever return zero--if we do it probably is a bug.
        // (this may need to be revisited in the future)
        Assertions.assertNotEquals(0, leftElement.hashCode())
        Assertions.assertNotEquals(0, rightElement.hashCode())

        // Check hashCode against normative implementation
        Assertions.assertEquals(hashElement(leftElement), leftElement.hashCode(), "hashCode() does not match normative implementation: ${leftElement::class.simpleName} $leftElement")
        Assertions.assertEquals(hashElement(rightElement), rightElement.hashCode(), "hashCode() does not match normative implementation: ${rightElement::class.simpleName} $rightElement")

        // Equivalence should be reflexive
        checkEquivalence(true, leftElement, leftElement)
        checkEquivalence(true, rightElement, rightElement)

        // Equivalence should be symmetric
        checkEquivalence(isEquiv, leftElement, rightElement)

        // Check reflexivity again, this time bypassing the reference equality check that happens first .equals calls
        checkEquivalence(true, leftElement, loader.loadSingleElement(left))
        checkEquivalence(true, rightElement, loader.loadSingleElement(right))

        // Adding annotations should not change the result
        val leftElementWithAnnotation = leftElement.withAnnotations("some_annotation")
        val rightElementWithAnnotation = rightElement.withAnnotations("some_annotation")
        checkEquivalence(
            isEquiv,
            leftElementWithAnnotation,
            rightElementWithAnnotation
        )

        // Adding an annotation to only one side will force them to be not equivalent
        checkEquivalence(false, leftElement.withAnnotations("some_annotation"), rightElement)

        // Adding metas has no effect
        checkEquivalence(isEquiv, leftElement.withMeta("foo", 1), rightElement)

        // Nesting the values within a struct should not change the result
        fun nest(ie: AnyElement) = ionStructOf("nested" to ie)
        checkEquivalence(isEquiv, nest(leftElement), nest(rightElement))
    }

    private fun checkEquivalence(equiv: Boolean, first: IonElement, second: IonElement) {
        if (equiv) {
            Assertions.assertTrue(areElementsEqual(first, second), "Elements should be equivalent.")
            Assertions.assertEquals(first, second, "equals() implementation does not match normative equivalence implementation")
            Assertions.assertEquals(first.hashCode(), second.hashCode(), "Elements' hash codes should be equal")
        } else {
            Assertions.assertFalse(areElementsEqual(first, second), "Elements should not be equivalent.")
            Assertions.assertNotEquals(first, second, "equals() implementation does not match normative equivalence implementation")
            // Note that two different [IonElement]s *can* have the same hash code and this might one day
            // break the build and may necessitate removing the assertion below. However, if it does happen we
            // should evaluate if the hashing algorithm is sufficient or not since that seems unlikely
            Assertions.assertNotEquals(first.hashCode(), second.hashCode(), "Elements' hash codes should not be equal")
        }
    }

    private fun IonElement.createProxy(): AnyElement {
        this as AnyElement
        return if (isNull) {
            object : AnyElement by this {}
        } else when (this) {
            is UnionOfBoolAndAnyElement -> object : UnionOfBoolAndAnyElement by this {}
            is UnionOfIntAndAnyElement -> object : UnionOfIntAndAnyElement by this {}
            is UnionOfFloatAndAnyElement -> object : UnionOfFloatAndAnyElement by this {}
            is UnionOfDecimalAndAnyElement -> object : UnionOfDecimalAndAnyElement by this {}
            is UnionOfTimestampAndAnyElement -> object : UnionOfTimestampAndAnyElement by this {}
            is UnionOfStringAndAnyElement -> object : UnionOfStringAndAnyElement by this {}
            is UnionOfSymbolAndAnyElement -> object : UnionOfSymbolAndAnyElement by this {}
            is UnionOfBlobAndAnyElement -> object : UnionOfBlobAndAnyElement by this {}
            is UnionOfClobAndAnyElement -> object : UnionOfClobAndAnyElement by this {}
            is UnionOfListAndAnyElement -> object : UnionOfListAndAnyElement by this {}
            is UnionOfSexpAndAnyElement -> object : UnionOfSexpAndAnyElement by this {}
            is UnionOfStructAndAnyElement -> object : UnionOfStructAndAnyElement by this {}
            else -> TODO("Unreachable")
        }
    }
}
