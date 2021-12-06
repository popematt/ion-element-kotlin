package com.amazon.ionelement

import com.amazon.ion.system.IonBinaryWriterBuilder
import com.amazon.ion.system.IonSystemBuilder
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class IonBinaryModelTest {
    val ion = IonSystemBuilder.standard().build()

    val checkDiffs = false
    val opts = PrettyPrintOptions(multiline = false, delimitingPunctuation = true)


    @ExperimentalUnsignedTypes
    @Test
    fun foo() {

        // runCase(""" [false, true, null.bool] """)

        // runCase(""" foo::bar::baz::[ null.string, null.null ] """)

        /*
        runCase(""" ["abc", "Hello World!", "Lorem ipsum dolor sit amet, consectetur adipiscing elit", "Quando Omni Flunkus Moritati"] """)
        runCase(""" ["a", ["b", "c"], "d", ["e", ["f", "g"]], "h"] """)
        runCase(""" { $2:"foo" } """)
        runCase(""" "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz" """)
        runCase(""" $2::"abcdefghijklmnopqrstuvwxyz" """)
        runCase(""" $2::$3::$4::"abc" """)
        runCase(""" ("add" 1 -2 3 0 null.int) """)
        runCase("1461501637330902918203684832716283019655932542976")
        runCase("192387461928734612374192834")
        runCase("0xFFFFFFFFFFFFFF")
        runCase("""{{ "abcd" }}""")
        */
        // runCase("""{ foo: bar }""")
//        runCase(
//            """
//            type::{
//              name: Person,
//              type: struct,
//              fields: {
//                title: {
//                  type: symbol,
//                  valid_values: [Mr, Mrs, Miss, Ms, Mx, Dr],
//                },
//                firstName: { type: string, occurs: required },
//                middleName: string,
//                lastName: { type: string, occurs: required },
//                age: { type: int, valid_values: range::[0, 130] },
//              },
//            }
//            """
//        )
//        runCase(
//            """
//              [
//                {
//                  firstName: "Horatio",
//                  lastName: "Hornblower",
//                },
//                {
//                  firstName: "Arthur",
//                  middleName: "J. M.",
//                  lastName: "Hastings",
//                },
//                {
//                  firstName: "James",
//                  middleName: "Harold",
//                  lastName: "Japp",
//                },
//                {
//                  firstName: "Felicity",
//                  lastName: "Lemon",
//                },
//                {
//                  firstName: "Túrin",
//                  lastName: "Turambar",
//                },
//                {
//                  firstName: "James",
//                  middleName: "Tiberius",
//                  lastName: "Kirk",
//                }
//              ]
//            """
//        )
        // runCase(""" [ foo::[1, 2], bar::{ a: b } ] """)

        runCase(" null ")
        val ionText = """ 
            [
                1234567890,
                9876543210,
                (a + b),
                {{ "fhqtscxvlrfhqtscxvlrfhqtscxvlr" }}, 
                "The quick brown fox...", 
                "...jumps over the lazy dog.",
                { a: b, c: "abcd", e: 1234 },
                false,
                true,
                foo::null,
                an::empty::list::[]
            ] """
        val optionsListB = listOf(
            PrettyPrintOptions(multiline = true),
            PrettyPrintOptions(multiline = true, textFormatting = TextFormat.ANSI_FONTS),
            PrettyPrintOptions(multiline = true, delimitingPunctuation = true, textFormatting = TextFormat.ANSI_COLOR),
            PrettyPrintOptions(multiline = false, textFormatting = TextFormat.ANSI_FONTS_COLORS),
            PrettyPrintOptions(multiline = true, delimitingPunctuation = true, textFormatting = TextFormat.HTML_FONTS),
            PrettyPrintOptions(multiline = false, textFormatting = TextFormat.HTML_BACKGROUNDS)
        )

        optionsListB.forEach { runCase(ionText, it) }
    }

    @ExperimentalUnsignedTypes
    private fun runCase(textIon: String, options: PrettyPrintOptions = opts) {
        val ionValue = ion.singleValue(textIon)

        val symbols = mutableListOf<String>()
        val ib = ionValue.toIonBinary(symbols)

        val datagram = IonBinaryDatagram(
            localSymbolTable = localSymbolTableOf(symbols),
            values = listOf(ib)
        )

        val ubytes = datagram.flatten()

        val os = ByteArrayOutputStream()
        val binaryWriter = IonBinaryWriterBuilder.standard().build(os)
        binaryWriter.use { ionValue.writeTo(it) }
        val bytes = os.toByteArray()


        if (checkDiffs && ubytes.toOctets() != bytes.toOctets()) {
            println("IonText: " + ionValue.toString())
            println("BinText: " + ubytes.toSpacedOctets())
            println("IonJava: " + bytes.toOctets().chunked(2).joinToString(" "))
        }
        println(datagram.toPrettyString(options))
        println()
    }
}
