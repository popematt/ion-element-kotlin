// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.ionelement.wrapper

import com.amazon.ion.IonValue
import com.amazon.ionelement.api.*
import com.amazon.ionelement.util.*
import kotlin.test.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

class IonValueWrapperTests {

    enum class GoodTestCase(val ionText: String) {
        `null`("null"),
        `typed null value`("null.bool"),
        `bool value`("true"),
        `int value`("123"),
        `big int value`("12345678901234567890"),
        `float value`("1e0"),
        `decimal value`("1d0"),
        `timestamp value`("2024-04-01T"),
        `string value`("\"foo\""),
        `symbol value`("bar"),
        `blob value`("{{ abc= }}"),
        `clob value`("{{ \"abc\" }}"),
        `list value`("[a,b,c]"),
        `sexp value`("(1 2 3)"),
        `struct value`("{a:1,b:2}"),
        `annotated value`("foo::bar::[]");

        fun testIt(block: (IonValue) -> Unit) = block(ION.singleValue(ionText))
    }

    @ParameterizedTest
    @EnumSource
    fun GoodTestCase.`wrapIntoIonElement should throw when a value is not already read-only`() = testIt {
        // Make sure that the value isn't already read-only. If it is, then assertion will have false results.
        check(!it.isReadOnly)
        assertThrows<IonElementException> { it.wrapIntoIonElement() }
    }

    @ParameterizedTest
    @EnumSource
    fun GoodTestCase.`wrapIntoIonElement should not throw when a value is already read-only`() = testIt {
        it.makeReadOnly()
        it.wrapIntoIonElement()
    }

    @ParameterizedTest
    @EnumSource
    fun GoodTestCase.`wrapIntoIonElement should wrap the same instance, not a copy`() = testIt {
        it.makeReadOnly()
        val wrapped = it.wrapIntoIonElement()
        if (wrapped is AnyValueWrapper<*>) {
            // unwrap should return the same instance
            assertSame(it, wrapped.unwrap())
        }
    }

    @ParameterizedTest
    @EnumSource
    fun GoodTestCase.`wrapUncheckedIntoIonElement should not throw if the value is read-only`() = testIt {
        it.makeReadOnly()
        it.wrapUncheckedIntoIonElement()
    }

    @ParameterizedTest
    @EnumSource
    fun GoodTestCase.`wrapUncheckedIntoIonElement should not throw even if the value is not read-only`() = testIt {
        check(!it.isReadOnly)
        it.wrapUncheckedIntoIonElement()
    }

    @ParameterizedTest
    @EnumSource
    fun GoodTestCase.`wrapUncheckedIntoIonElement should wrap the same instance, not a copy`() = testIt {
        val wrapped = it.wrapUncheckedIntoIonElement()
        if (wrapped is AnyValueWrapper<*>) {
            // unwrap should return the same instance
            assertSame(it, wrapped.unwrap())
            // ... and the value should now be read-only because AnyValueWrapper must only delegate to read only values
            assertTrue(wrapped.unwrap().isReadOnly)
        }
    }

    @ParameterizedTest
    @EnumSource
    fun GoodTestCase.`a wrapped IonValue should be equal to the equivalent IonElement`() = testIt {
        val wrapped = it.wrapUncheckedIntoIonElement()
        val converted = it.toIonElement()
        assertEquals(converted, wrapped)
        assertEquals(converted.hashCode(), wrapped.hashCode())
    }

    @ParameterizedTest
    @EnumSource
    fun GoodTestCase.`a wrapped IonValue should serialize equivalently to IonElement`() = testIt {
        val wrapped = it.wrapUncheckedIntoIonElement()
        val converted = it.toIonElement()
        assertEquals(loadSingleElement(converted.toString()), loadSingleElement(wrapped.toString()))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "$0",
            "{$0:1}",
            "$0::[]",
            "[$0]",
            "[{$0:1}]",
            "[$0::[]]",
        ]
    )
    fun `when wrapping an unknown symbol, should throw exception`(ionText: String) {
        val ionValue = ION.singleValue(ionText)

        assertThrows<IonElementException> {
            val wrapped = ionValue.wrapUncheckedIntoIonElement()
            // When the unknown symbol is an annotation, it might not throw until the annotations are accessed.
            wrapped.annotations
            // When the unknown symbol is in a container, it doesn't have to throw until the container values are accessed.
            if (wrapped is ContainerElement) { wrapped.values }
            fail("This should be unreachable.")
        }
    }

    @Test
    fun `attempting to wrap a datagram should throw an exception`() {
        val datagram = ION.loader.load("foo bar")
        assertThrows<IonElementWrapperException> {
            datagram.wrapUncheckedIntoIonElement()
        }
    }
}
