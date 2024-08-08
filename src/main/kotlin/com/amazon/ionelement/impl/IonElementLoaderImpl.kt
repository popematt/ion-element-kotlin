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

package com.amazon.ionelement.impl

import com.amazon.ion.IntegerSize
import com.amazon.ion.IonException
import com.amazon.ion.IonReader
import com.amazon.ion.IonType
import com.amazon.ion.OffsetSpan
import com.amazon.ion.SpanProvider
import com.amazon.ion.TextSpan
import com.amazon.ion.system.IonReaderBuilder
import com.amazon.ionelement.api.*
import com.amazon.ionelement.impl.collections.*
import java.util.ArrayDeque
import java.util.ArrayList
import kotlinx.collections.immutable.adapters.ImmutableListAdapter

internal class IonElementLoaderImpl(private val options: IonElementLoaderOptions) : IonElementLoader {

    /**
     * Catches an [IonException] occurring in [block] and throws an [IonElementLoaderException] with
     * the current [IonLocation] of the fault, if one is available.  Note that depending on the state of the
     * [IonReader], a location may in fact not be available.
     */
    private inline fun <T> handleReaderException(ionReader: IonReader, crossinline block: () -> T): T {
        try {
            return block()
        } catch (e: IonException) {
            throw IonElementException(
                location = ionReader.currentLocation(),
                description = "IonException occurred, likely due to malformed Ion data (see cause)",
                cause = e
            )
        }
    }

    private fun IonReader.currentLocation(): IonLocation? =
        when {
            // Can't attempt to get a SpanProvider unless we're on a value
            this.type == null -> null
            else -> {
                val spanFacet = this.asFacet(SpanProvider::class.java)
                when (val currentSpan = spanFacet.currentSpan()) {
                    is TextSpan -> IonTextLocation(currentSpan.startLine, currentSpan.startColumn)
                    is OffsetSpan -> IonBinaryLocation(currentSpan.startOffset)
                    else -> null
                }
            }
        }

    override fun loadSingleElement(ionText: String): AnyElement =
        IonReaderBuilder.standard().build(ionText).use(::loadSingleElement)

    override fun loadSingleElement(ionReader: IonReader): AnyElement {
        return handleReaderException(ionReader) {
            ionReader.next()
            loadCurrentElement(ionReader).also {
                ionReader.next()
                require(ionReader.type == null) { "More than a single value was present in the specified IonReader." }
            }
        }
    }

    override fun loadAllElements(ionReader: IonReader): List<AnyElement> {
        return handleReaderException(ionReader) {
            val elements = mutableListOf<AnyElement>()
            while (ionReader.next() != null) {
                val depth = ionReader.depth
                elements.add(loadCurrentElement(ionReader))
                check(depth == ionReader.depth)
            }
            elements
        }
    }

    override fun loadAllElements(ionText: String): List<AnyElement> =
        IonReaderBuilder.standard().build(ionText).use(::loadAllElements)

    override fun loadCurrentElement(ionReader: IonReader): AnyElement {
        return private_loadCurrentElement(ionReader)
    }

    private fun private_loadCurrentElement(ionReader: IonReader): AnyElement {
        return handleReaderException(ionReader) {
            val valueType = requireNotNull(ionReader.type) { "The IonReader was not positioned at an element." }

            // Read a value
            val annotations = ionReader.typeAnnotations!!.toImmutableListUnsafe()

            var metas = EMPTY_METAS
            if (options.includeLocationMeta) {
                val location = ionReader.currentLocation()
                if (location != null) metas = location.toMetaContainer()
            }

            val element = if (ionReader.isNullValue) {
                    ionNull(valueType.toElementType(), annotations, metas).asAnyElement()
                } else when (valueType) {
                    IonType.BOOL -> BoolElementImpl(ionReader.booleanValue(), annotations, metas)
                    IonType.INT -> when (ionReader.integerSize!!) {
                        IntegerSize.BIG_INTEGER -> {
                            val bigIntValue = ionReader.bigIntegerValue()
                            // Ion java's IonReader appears to determine integerSize based on number of bits,
                            // not on the actual value, which means if we have a padded int that is > 63 bits,
                            // but whose value only uses <= 63 bits then integerSize is still BIG_INTEGER.
                            // Compensate for that here...
                            if (bigIntValue !in RANGE_OF_LONG)
                                BigIntIntElementImpl(bigIntValue, annotations, metas)
                            else {
                                LongIntElementImpl(ionReader.longValue(), annotations, metas)
                            }
                        }

                        IntegerSize.LONG,
                        IntegerSize.INT -> LongIntElementImpl(ionReader.longValue(), annotations, metas)
                    }

                    IonType.FLOAT -> FloatElementImpl(ionReader.doubleValue(), annotations, metas)
                    IonType.DECIMAL -> DecimalElementImpl(ionReader.decimalValue(), annotations, metas)
                    IonType.TIMESTAMP -> TimestampElementImpl(ionReader.timestampValue(), annotations, metas)
                    IonType.STRING -> StringElementImpl(ionReader.stringValue(), annotations, metas)
                    IonType.SYMBOL -> SymbolElementImpl(ionReader.stringValue(), annotations, metas)
                    IonType.CLOB -> ClobElementImpl(ionReader.newBytes(), annotations, metas)
                    IonType.BLOB -> BlobElementImpl(ionReader.newBytes(), annotations, metas)
                    IonType.LIST -> {
                        val listContent = mutableListOf<AnyElement>()
                        val depth = ionReader.depth
                        ionReader.stepIn()
                        private_loadAllElements(ionReader, listContent as MutableList<Any>)
                        ionReader.stepOut()
                        check(depth == ionReader.depth)
                        ListElementImpl(listContent.toImmutableListUnsafe(), annotations, metas)
                    }

                    IonType.SEXP -> {
                        val sexpContent = mutableListOf<AnyElement>()
                        val depth = ionReader.depth
                        ionReader.stepIn()
                        private_loadAllElements(ionReader, sexpContent as MutableList<Any>)
                        ionReader.stepOut()
                        check(depth == ionReader.depth)
                        SexpElementImpl(sexpContent.toImmutableListUnsafe(), annotations, metas)
                    }

                    IonType.STRUCT -> {
                        val structContent = mutableListOf<StructField>()
                        val depth = ionReader.depth
                        ionReader.stepIn()
                        private_loadAllElements(ionReader, structContent as MutableList<Any>)
                        ionReader.stepOut()
                        check(depth == ionReader.depth)
                        StructElementImpl(structContent.toImmutableListUnsafe(), annotations, metas)
                    }

                    IonType.DATAGRAM -> error("IonElementLoaderImpl does not know what to do with IonType.DATAGRAM")
                    IonType.NULL -> error("IonType.NULL branch should be unreachable")

                }.asAnyElement()
            element
        }
    }

    private fun private_loadAllElements(ionReader: IonReader, into: MutableList<Any>) {
        // Intentionally not a recycling stack because we have mutable references that we are going to wrap as an
        // ImmutableList and then forget about the reference to the mutable list.
        val sequenceContentStack = ArrayDeque<MutableList<AnyElement>>()
        val fieldsStack = ArrayDeque<MutableList<StructFieldImpl>>()
        var elements: MutableList<Any> = into
        var nextElements: MutableList<Any> = elements

        try {
            while (true) {
                val valueType = ionReader.next()

                // End of container or input
                if (valueType == null) {
                    if (sequenceContentStack.isEmpty() && fieldsStack.isEmpty()) {
                        return
                    } else {
                        ionReader.stepOut()
                        nextElements = if (ionReader.isInStruct) {
                            fieldsStack.pop() as MutableList<Any>
                        } else {
                            sequenceContentStack.pop() as MutableList<Any>
                        }
                        elements = nextElements
                        continue
                    }
                }

                // Read a value
                val annotations = ionReader.typeAnnotations!!.toImmutableListUnsafe()

                var metas = EMPTY_METAS
                if (options.includeLocationMeta) {
                    val location = ionReader.currentLocation()
                    if (location != null) metas = location.toMetaContainer()
                }

                val element = if (ionReader.isNullValue) {
                    ionNull(valueType.toElementType(), annotations, metas).asAnyElement()
                } else when (valueType) {
                    IonType.BOOL -> BoolElementImpl(ionReader.booleanValue(), annotations, metas)
                    IonType.INT -> when (ionReader.integerSize!!) {
                        IntegerSize.BIG_INTEGER -> {
                            val bigIntValue = ionReader.bigIntegerValue()
                            // Ion java's IonReader appears to determine integerSize based on number of bits,
                            // not on the actual value, which means if we have a padded int that is > 63 bits,
                            // but whose value only uses <= 63 bits then integerSize is still BIG_INTEGER.
                            // Compensate for that here...
                            if (bigIntValue !in RANGE_OF_LONG)
                                BigIntIntElementImpl(bigIntValue, annotations, metas)
                            else {
                                LongIntElementImpl(ionReader.longValue(), annotations, metas)
                            }
                        }

                        IntegerSize.LONG,
                        IntegerSize.INT -> LongIntElementImpl(ionReader.longValue(), annotations, metas)
                    }

                    IonType.FLOAT -> FloatElementImpl(ionReader.doubleValue(), annotations, metas)
                    IonType.DECIMAL -> DecimalElementImpl(ionReader.decimalValue(), annotations, metas)
                    IonType.TIMESTAMP -> TimestampElementImpl(ionReader.timestampValue(), annotations, metas)
                    IonType.STRING -> StringElementImpl(ionReader.stringValue(), annotations, metas)
                    IonType.SYMBOL -> SymbolElementImpl(ionReader.stringValue(), annotations, metas)
                    IonType.CLOB -> ClobElementImpl(ionReader.newBytes(), annotations, metas)
                    IonType.BLOB -> BlobElementImpl(ionReader.newBytes(), annotations, metas)
                    IonType.LIST -> {
                        val listContent = ArrayList<AnyElement>()
                        nextElements = listContent as MutableList<Any>
                        ListElementImpl(ImmutableListAdapter(listContent), annotations, metas)
                    }

                    IonType.SEXP -> {
                        val sexpContent = ArrayList<AnyElement>()
                        nextElements = sexpContent as MutableList<Any>
                        SexpElementImpl(ImmutableListAdapter(sexpContent), annotations, metas)
                    }

                    IonType.STRUCT -> {
                        val structContent = ArrayList<StructField>()
                        nextElements = structContent as MutableList<Any>
                        StructElementImpl(ImmutableListAdapter(structContent), annotations, metas)
                    }

                    IonType.DATAGRAM -> error("IonElementLoaderImpl does not know what to do with IonType.DATAGRAM")
                    IonType.NULL -> error("IonType.NULL branch should be unreachable")

                }.asAnyElement()

                if (ionReader.isInStruct) {
                    elements.add(StructFieldImpl(ionReader.fieldName, element))
                } else {
                    elements.add(element)
                }

                // Step in, if necessary
                if (IonType.isContainer(valueType)) {
                    if (ionReader.isInStruct) {
                        fieldsStack.push(elements as MutableList<StructFieldImpl>)
                    } else {
                        sequenceContentStack.push(elements as MutableList<AnyElement>)
                    }
                    ionReader.stepIn()
                }
                elements = nextElements
            }

        } catch (e: IonException) {
            throw IonElementException(
                location = ionReader.currentLocation(),
                description = "IonException occurred, likely due to malformed Ion data (see cause)",
                cause = e
            )
        }
    }
}
