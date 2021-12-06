package com.amazon.ionelement.encoding.text

import com.amazon.ionelement.api.loadSingleElement
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class IonTextTokenTest {

    @Test
    fun testToAnyElement() {
        val input = """
            [
              "a", // The first letter of the alphabet
              "b", // The second letter of the alphabet
              "c", // The third letter of the alphabet
            ]
            """.trimIndent()


        val tokens = readTokens(input)

        val forest = buildForest(tokens)

        val actual = forest.toAnyElement()

        val expected = loadSingleElement(input)

        assertEquals(expected, actual)
    }

    @Test
    fun treeTest() {
        val input = """
            {
              // Foo
              a:"b",
              b: [
                "c",
                "d"
              ]
            }
            """.trimIndent()


        val tokens = readTokens(input)

        val forest = buildForest(tokens)

        println("""(document ${forest.joinToString(" ") {it.toSexpString()}})""")
    }

    @Test
    fun foo() {
        val input = """
/* Ion supports comments. */
// Here is a struct, which is similar to a JSON object
{
  // Field names don't always have to be quoted
  name: "Fido",

  // This is an integer with a 'years' annotation
  age: years::4,

  // This is a timestamp with day precision
  birthday: 2012-03-01T,

  // Here is a list, which is like a JSON array
  toys: [
    // These are symbol values, which are like strings,
    // but get encoded as integers in binary
    ball,
    rope,
  ],

  // This is a decimal -- a base-10 floating point value
  weight: pounds::41.2,

  // Here is a blob -- binary data, which is
  // base64-encoded in Ion text encoding
  buzz: {{VG8gaW5maW5pdHkuLi4gYW5kIGJleW9uZCE=}},
  something: null.bool,
}

        """.trimIndent()

        val tokens = readTokens(input)

        println(tokens.joinToString("\n"){ it.toString().replace("\n", "\\n")})

        val forest = buildForest(tokens)

        println("""(document ${forest.joinToString(" ") {it.toSexpString(1)}})""")

        println("Size is the same: ${tokens.size == forest.sumBy { it.traverseTokens().count() }}")
    }

    @Test
    fun testCollect() {
        val str = " abcde    "
        val range = str.collectUntil(1) { i -> this[i].isWhitespace() }
        assertEquals("abcde", str[range])
        assertEquals(1 until 6, range)
    }
}
