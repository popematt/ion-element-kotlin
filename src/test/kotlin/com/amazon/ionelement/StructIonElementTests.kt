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
import com.amazon.ionelement.util.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class StructElementImplTests : StructIonElementTests() {
    override val struct = loadSingleElement(structText).asStruct()
}

class StructValueWrapperTests : StructIonElementTests() {
    override val struct = ION.singleValue(structText).wrapUncheckedIntoIonElement().asStruct()
}

abstract class StructIonElementTests {
    protected val structText = "{ a: 1, b: 2, b: 3, b: 3 }"
    abstract val struct: StructElement

    @Test
    fun size() {
        assertEquals(4, struct.size, "struct size should be 4")
    }

    @Test
    fun fields() {
        val structFields = struct.fields.toList()
        assertEquals(4, structFields.size)
        structFields.assertHasField("a", ionInt(1))
        structFields.assertHasField("b", ionInt(2))
        structFields.assertHasField("b", ionInt(3), expectedCount = 2)
    }

    @Test
    fun values() {
        val values = struct.values.toList()
        assertEquals(4, values.size, "4 values should be present")
        assertDoesNotThrow("value 1 should be present") { values.single { it.longValue == 1L } }
        assertDoesNotThrow("value 2 should be present") { values.single { it.longValue == 2L } }
        assertTrue(values.count { it.longValue == 3L } == 2, "value 3 should be present")
    }

    @Test
    fun get() {
        assertEquals(
            ionInt(1), struct["a"],
            "value is returned when field is present"
        )

        val b1 = struct["b"]
        assertTrue(
            listOf(ionInt(2), ionInt(3)).any { it == b1 },
            "any value of the b field is returned (duplicate field name)"
        )

        val ex = assertThrows<IonElementException>("exception is thrown when field is not present") {
            struct["z"]
        }
        assertTrue(
            ex.message!!.contains("'z'"),
            "Exception message must contain the missing field"
        )
    }

    @Test
    fun getOptional() {
        assertEquals(
            ionInt(1), struct.getOptional("a"),
            "value is returned when field is present"
        )

        val b2 = struct.getOptional("b")
        assertTrue(
            listOf(ionInt(2), ionInt(3)).any { it == b2 },
            "any value of the b field is returned (duplicate field name)"
        )

        assertNull(
            struct.getOptional("z"),
            "null is returned when the field is not present."
        )
    }

    @Test
    fun getAll() {
        val expectedValueCounts = mapOf(
            // The value 2 should occur once in the list
            ionInt(2) to 1,
            // The value 3 should occur twice in the list
            ionInt(3) to 2,
        )
        assertEquals(expectedValueCounts, struct.getAll("b").groupingBy { it }.eachCount())

        assertEquals(emptyList<AnyElement>(), struct.getAll("z"))
    }

    @Test
    fun containsField() {
        assertTrue(struct.containsField("a"))
        assertTrue(struct.containsField("b"))
        assertFalse(struct.containsField("z"))
    }

    private fun Iterable<StructField>.assertHasField(expectedName: String, expectedValue: IonElement, expectedCount: Int = 1) {
        val expectedField = field(expectedName, expectedValue)
        val actualCount = this.count { it == expectedField }
        assertTrue(actualCount > 0, "Must have field $expectedField")
        assertTrue(actualCount == expectedCount, "Expected $expectedField $expectedCount times; found $actualCount")
    }
}
