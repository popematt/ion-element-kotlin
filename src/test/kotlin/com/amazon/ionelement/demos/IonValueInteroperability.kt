package com.amazon.ionelement.demos

import com.amazon.ion.IonStruct
import com.amazon.ion.IonValue
import com.amazon.ionelement.api.*
import com.amazon.ionelement.impl.*
import com.amazon.ionelement.util.*
import com.amazon.ionelement.wrapper.*
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

/**
 * Demonstrates how [IonElement] can be used in an application that has dependencies
 * that use [IonValue], and vice versa.
 */
class IonValueInteroperability {
    @Test
    fun foo() {

        val evenList: IonElement = loadSingleElement("[2, 4, 6]")
        val oddList: IonElement = loadSingleElement("[1, 3, 5]")
        val ionValueOddList: IonValue = ION.singleValue("[1, 3, 5]").apply { makeReadOnly() }

        val evensAndOdds = ionStructOf(
            "evens" to evenList,
            "odds" to oddList
        )

        // Let's wrap the `ionValueOddList` as an IonElement
        val wrappedOddList = ionValueOddList.wrapIntoIonElement()

        // We can compare them for equality
        assertEquals(oddList, wrappedOddList)
        // They have the same hashcode
        assertEquals(oddList.hashCode(), wrappedOddList.hashCode())
        // They serialize the same way
        assertEquals(oddList.toString(), wrappedOddList.toString())

        // We can add the wrapped odd list to an StructElement, and expect the same behavior
        val evensAndOddsB = evensAndOdds.update { set("odds", wrappedOddList) }
        assertEquals(evensAndOdds, evensAndOddsB)
        assertEquals(evensAndOdds.hashCode(), evensAndOddsB.hashCode())
        assertEquals(evensAndOdds.toString(), evensAndOddsB.toString())

        // This is not public functionality (yet), but it demonstrates that we're still using
        // the wrapped IonValue rather than eagerly converting it.
        assertSame(ionValueOddList, (evensAndOddsB["odds"] as IonValueWrapper).unwrap())
    }

    @Test
    fun bar() {

        val ionValueEvens: IonStruct = ION.singleValue("{ evens: [2, 4, 6] }").apply { makeReadOnly() } as IonStruct
        val oddList: IonElement = loadSingleElement("[1, 3, 5]")

        val evensAndOdds = ionValueEvens.wrapIntoIonElement().asStruct().update {
            set("odds", oddList)
        }


        // These verifications rely on non-public functionality

        // The outer struct has been seamlessly converted to `StructElement`
        assertTrue(evensAndOdds is StructElementImpl)

        // ... but we're still using the wrapped IonValue for the evens list.
        assertSame(ionValueEvens.get("evens"), (evensAndOdds["evens"] as IonValueWrapper).unwrap())
    }

}
