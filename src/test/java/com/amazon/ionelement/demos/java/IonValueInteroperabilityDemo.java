package com.amazon.ionelement.demos.java;

import com.amazon.ion.IonStruct;
import com.amazon.ion.IonValue;
import com.amazon.ionelement.api.*;
import com.amazon.ionelement.impl.StructElementImpl;
import com.amazon.ionelement.util.TestUtils;
import com.amazon.ionelement.wrapper.IonValueWrapper;
import org.junit.jupiter.api.Test;

import static com.amazon.ionelement.api.Ion.field;
import static org.junit.jupiter.api.Assertions.*;

public class IonValueInteroperabilityDemo {

    @Test
    void addWrappedIonListToStructElement() {
        IonElement oddList = ElementLoader.loadSingleElement("[1, 3, 5]");
        IonElement evenList = ElementLoader.loadSingleElement("[2, 4, 6]");
        IonValue ionValueOddList = TestUtils.ION.singleValue("[1, 3, 5]");
        ionValueOddList.makeReadOnly();

        StructElement evensAndOdds = Ion.ionStructOf(
                field("evens", evenList),
                field("odds", oddList)
        );

        IonElement wrappedOddList = IonUtils.wrapUncheckedIntoIonElement(ionValueOddList);


        // We can compare them for equality
        assertEquals(oddList, wrappedOddList);
        // They have the same hashcode
        assertEquals(oddList.hashCode(), wrappedOddList.hashCode());
        // They even serialize the same way
        assertEquals(oddList.toString(), wrappedOddList.toString());

        // We can add the wrapped odd list to an StructElement, and expect the same behavior
        StructElement evensAndOddsB = evensAndOdds.update(fields -> fields.set("odds", wrappedOddList));
        assertEquals(evensAndOdds, evensAndOddsB);
        assertEquals(evensAndOdds.hashCode(), evensAndOddsB.hashCode());
        assertEquals(evensAndOdds.toString(), evensAndOddsB.toString());


        // This is not public functionality, but it demonstrates that we're still using
        // the wrapped IonValue rather than eagerly converting it.
        assertSame(ionValueOddList, ((IonValueWrapper) evensAndOddsB.get("odds")).unwrap());
    }

    @Test
    void addListElementToWrappedIonStruct() {
        IonStruct ionValueEvens = (IonStruct) TestUtils.ION.singleValue("{ evens: [2, 4, 6] }");
        ionValueEvens.makeReadOnly();

        ListElement oddList = ElementLoader.loadSingleElement("[1, 3, 5]").asList();

        StructElement evensAndOdds = IonUtils.wrapUncheckedIntoIonElement(ionValueEvens)
                .asStruct()
                .update(fields -> fields.set("odds", oddList));

        // These verifications rely on non-public functionality
        // The outer struct has been seamlessly converted to `StructElement`
        assertTrue(evensAndOdds instanceof StructElementImpl);
        // ... but we're still using the wrapped IonValue for the evens list.
        assertSame(ionValueEvens.get("evens"), ((IonValueWrapper) evensAndOdds.get("evens")).unwrap());
    }
}
