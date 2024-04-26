// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.ionelement

import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ionelement.api.*
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class ToIonValueTests {

    /**
     * This test is needed due to the way `IonElement.toIonValue(ValueFactory)` uses a temporary container to obtain
     * an `IonWriter` implementation to which the receiver will be written.
     */
    @Test
    fun `IonValue instances converted from IonElement instances can be added to IonContainer instances`() {
        val ion = IonSystemBuilder.standard().build()
        val ionValue = ion.singleValue("1")
        val element = ionValue.toIonElement()
        val ionList = ion.newList(element.toIonValue(ion))

        assertEquals(ion.singleValue("[1]"), ionList)
    }

    @Test
    fun `IonValue instances converted from IonValueWrapper instances can be added to IonContainer instances`() {
        val ion = IonSystemBuilder.standard().build()
        val ionValue = ion.singleValue("1")
        val element = ionValue.wrapUncheckedIntoIonElement()
        val ionList = ion.newList(element.toIonValue(ion))

        assertEquals(ion.singleValue("[1]"), ionList)
    }

    @Test
    fun `getting a read-only IonValue instance from IonElement instance a read-only instance`() {
        val ion = IonSystemBuilder.standard().build()
        val ionValue = ion.singleValue("[1]")
        val element = ionValue.toIonElement()
        val readOnlyIonValue = element.toReadOnlyIonValue(ion)

        assertTrue(readOnlyIonValue.isReadOnly)
    }

    @Test
    fun `getting a read-only IonValue instance from IonValueWrapper instance returns the same instance`() {
        val ion = IonSystemBuilder.standard().build()
        val ionValue = ion.singleValue("[1]")
        val element = ionValue.wrapUncheckedIntoIonElement()
        val readOnlyIonValue = element.toReadOnlyIonValue(ion)

        assertTrue(readOnlyIonValue.isReadOnly)
        assertSame(ionValue, readOnlyIonValue)
    }
}
