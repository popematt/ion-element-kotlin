package com.amazon.ionelement.impl

import com.amazon.ionelement.api.*

// Macros or smarter type bounds would be nice here.

internal interface UnionOfBoolAndAnyElement: BoolElement, AnyElement {
    override fun copy(annotations: List<String>, metas: MetaContainer): UnionOfBoolAndAnyElement
    override fun withAnnotations(vararg additionalAnnotations: String): UnionOfBoolAndAnyElement = _withAnnotations(*additionalAnnotations)
    override fun withAnnotations(additionalAnnotations: Iterable<String>): UnionOfBoolAndAnyElement = _withAnnotations(additionalAnnotations)
    override fun withoutAnnotations(): UnionOfBoolAndAnyElement = _withoutAnnotations()
    override fun withMetas(additionalMetas: MetaContainer): UnionOfBoolAndAnyElement = _withMetas(additionalMetas)
    override fun withMeta(key: String, value: Any): UnionOfBoolAndAnyElement = _withMeta(key, value)
    override fun withoutMetas(): UnionOfBoolAndAnyElement = _withoutMetas()
}

internal interface UnionOfIntAndAnyElement: IntElement, AnyElement {
    override fun copy(annotations: List<String>, metas: MetaContainer): UnionOfIntAndAnyElement
    override fun withAnnotations(vararg additionalAnnotations: String): UnionOfIntAndAnyElement = _withAnnotations(*additionalAnnotations)
    override fun withAnnotations(additionalAnnotations: Iterable<String>): UnionOfIntAndAnyElement = _withAnnotations(additionalAnnotations)
    override fun withoutAnnotations(): UnionOfIntAndAnyElement = _withoutAnnotations()
    override fun withMetas(additionalMetas: MetaContainer): UnionOfIntAndAnyElement = _withMetas(additionalMetas)
    override fun withMeta(key: String, value: Any): UnionOfIntAndAnyElement = _withMeta(key, value)
    override fun withoutMetas(): UnionOfIntAndAnyElement = _withoutMetas()
}

internal interface UnionOfFloatAndAnyElement: FloatElement, AnyElement {
    override fun copy(annotations: List<String>, metas: MetaContainer): UnionOfFloatAndAnyElement
    override fun withAnnotations(vararg additionalAnnotations: String): UnionOfFloatAndAnyElement = _withAnnotations(*additionalAnnotations)
    override fun withAnnotations(additionalAnnotations: Iterable<String>): UnionOfFloatAndAnyElement = _withAnnotations(additionalAnnotations)
    override fun withoutAnnotations(): UnionOfFloatAndAnyElement = _withoutAnnotations()
    override fun withMetas(additionalMetas: MetaContainer): UnionOfFloatAndAnyElement = _withMetas(additionalMetas)
    override fun withMeta(key: String, value: Any): UnionOfFloatAndAnyElement = _withMeta(key, value)
    override fun withoutMetas(): UnionOfFloatAndAnyElement = _withoutMetas()
}

internal interface UnionOfDecimalAndAnyElement: DecimalElement, AnyElement {
    override fun copy(annotations: List<String>, metas: MetaContainer): UnionOfDecimalAndAnyElement
    override fun withAnnotations(vararg additionalAnnotations: String): UnionOfDecimalAndAnyElement = _withAnnotations(*additionalAnnotations)
    override fun withAnnotations(additionalAnnotations: Iterable<String>): UnionOfDecimalAndAnyElement = _withAnnotations(additionalAnnotations)
    override fun withoutAnnotations(): UnionOfDecimalAndAnyElement = _withoutAnnotations()
    override fun withMetas(additionalMetas: MetaContainer): UnionOfDecimalAndAnyElement = _withMetas(additionalMetas)
    override fun withMeta(key: String, value: Any): UnionOfDecimalAndAnyElement = _withMeta(key, value)
    override fun withoutMetas(): UnionOfDecimalAndAnyElement = _withoutMetas()
}

internal interface UnionOfTimestampAndAnyElement: TimestampElement, AnyElement {
    override fun copy(annotations: List<String>, metas: MetaContainer): UnionOfTimestampAndAnyElement
    override fun withAnnotations(vararg additionalAnnotations: String): UnionOfTimestampAndAnyElement = _withAnnotations(*additionalAnnotations)
    override fun withAnnotations(additionalAnnotations: Iterable<String>): UnionOfTimestampAndAnyElement = _withAnnotations(additionalAnnotations)
    override fun withoutAnnotations(): UnionOfTimestampAndAnyElement = _withoutAnnotations()
    override fun withMetas(additionalMetas: MetaContainer): UnionOfTimestampAndAnyElement = _withMetas(additionalMetas)
    override fun withMeta(key: String, value: Any): UnionOfTimestampAndAnyElement = _withMeta(key, value)
    override fun withoutMetas(): UnionOfTimestampAndAnyElement = _withoutMetas()
}

internal interface UnionOfStringAndAnyElement: StringElement, AnyElement {
    override fun copy(annotations: List<String>, metas: MetaContainer): UnionOfStringAndAnyElement
    override fun withAnnotations(vararg additionalAnnotations: String): UnionOfStringAndAnyElement = _withAnnotations(*additionalAnnotations)
    override fun withAnnotations(additionalAnnotations: Iterable<String>): UnionOfStringAndAnyElement = _withAnnotations(additionalAnnotations)
    override fun withoutAnnotations(): UnionOfStringAndAnyElement = _withoutAnnotations()
    override fun withMetas(additionalMetas: MetaContainer): UnionOfStringAndAnyElement = _withMetas(additionalMetas)
    override fun withMeta(key: String, value: Any): UnionOfStringAndAnyElement = _withMeta(key, value)
    override fun withoutMetas(): UnionOfStringAndAnyElement = _withoutMetas()
}

internal interface UnionOfSymbolAndAnyElement: SymbolElement, AnyElement {
    override fun copy(annotations: List<String>, metas: MetaContainer): UnionOfSymbolAndAnyElement
    override fun withAnnotations(vararg additionalAnnotations: String): UnionOfSymbolAndAnyElement = _withAnnotations(*additionalAnnotations)
    override fun withAnnotations(additionalAnnotations: Iterable<String>): UnionOfSymbolAndAnyElement = _withAnnotations(additionalAnnotations)
    override fun withoutAnnotations(): UnionOfSymbolAndAnyElement = _withoutAnnotations()
    override fun withMetas(additionalMetas: MetaContainer): UnionOfSymbolAndAnyElement = _withMetas(additionalMetas)
    override fun withMeta(key: String, value: Any): UnionOfSymbolAndAnyElement = _withMeta(key, value)
    override fun withoutMetas(): UnionOfSymbolAndAnyElement = _withoutMetas()
}

internal interface UnionOfBlobAndAnyElement: BlobElement, AnyElement {
    override fun copy(annotations: List<String>, metas: MetaContainer): UnionOfBlobAndAnyElement
    override fun withAnnotations(vararg additionalAnnotations: String): UnionOfBlobAndAnyElement = _withAnnotations(*additionalAnnotations)
    override fun withAnnotations(additionalAnnotations: Iterable<String>): UnionOfBlobAndAnyElement = _withAnnotations(additionalAnnotations)
    override fun withoutAnnotations(): UnionOfBlobAndAnyElement = _withoutAnnotations()
    override fun withMetas(additionalMetas: MetaContainer): UnionOfBlobAndAnyElement = _withMetas(additionalMetas)
    override fun withMeta(key: String, value: Any): UnionOfBlobAndAnyElement = _withMeta(key, value)
    override fun withoutMetas(): UnionOfBlobAndAnyElement = _withoutMetas()
}

internal interface UnionOfClobAndAnyElement: ClobElement, AnyElement {
    override fun copy(annotations: List<String>, metas: MetaContainer): UnionOfClobAndAnyElement
    override fun withAnnotations(vararg additionalAnnotations: String): UnionOfClobAndAnyElement = _withAnnotations(*additionalAnnotations)
    override fun withAnnotations(additionalAnnotations: Iterable<String>): UnionOfClobAndAnyElement = _withAnnotations(additionalAnnotations)
    override fun withoutAnnotations(): UnionOfClobAndAnyElement = _withoutAnnotations()
    override fun withMetas(additionalMetas: MetaContainer): UnionOfClobAndAnyElement = _withMetas(additionalMetas)
    override fun withMeta(key: String, value: Any): UnionOfClobAndAnyElement = _withMeta(key, value)
    override fun withoutMetas(): UnionOfClobAndAnyElement = _withoutMetas()
}

internal interface UnionOfListAndAnyElement: ListElement, AnyElement {
    override fun copy(annotations: List<String>, metas: MetaContainer): UnionOfListAndAnyElement
    override fun withAnnotations(vararg additionalAnnotations: String): UnionOfListAndAnyElement = _withAnnotations(*additionalAnnotations)
    override fun withAnnotations(additionalAnnotations: Iterable<String>): UnionOfListAndAnyElement = _withAnnotations(additionalAnnotations)
    override fun withoutAnnotations(): UnionOfListAndAnyElement = _withoutAnnotations()
    override fun withMetas(additionalMetas: MetaContainer): UnionOfListAndAnyElement = _withMetas(additionalMetas)
    override fun withMeta(key: String, value: Any): UnionOfListAndAnyElement = _withMeta(key, value)
    override fun withoutMetas(): UnionOfListAndAnyElement = _withoutMetas()
}

internal interface UnionOfSexpAndAnyElement: SexpElement, AnyElement {
    override fun copy(annotations: List<String>, metas: MetaContainer): UnionOfSexpAndAnyElement
    override fun withAnnotations(vararg additionalAnnotations: String): UnionOfSexpAndAnyElement = _withAnnotations(*additionalAnnotations)
    override fun withAnnotations(additionalAnnotations: Iterable<String>): UnionOfSexpAndAnyElement = _withAnnotations(additionalAnnotations)
    override fun withoutAnnotations(): UnionOfSexpAndAnyElement = _withoutAnnotations()
    override fun withMetas(additionalMetas: MetaContainer): UnionOfSexpAndAnyElement = _withMetas(additionalMetas)
    override fun withMeta(key: String, value: Any): UnionOfSexpAndAnyElement = _withMeta(key, value)
    override fun withoutMetas(): UnionOfSexpAndAnyElement = _withoutMetas()
}

internal interface UnionOfStructAndAnyElement: StructElement, AnyElement {
    override fun copy(annotations: List<String>, metas: MetaContainer): UnionOfStructAndAnyElement
    override fun withAnnotations(vararg additionalAnnotations: String): UnionOfStructAndAnyElement = _withAnnotations(*additionalAnnotations)
    override fun withAnnotations(additionalAnnotations: Iterable<String>): UnionOfStructAndAnyElement = _withAnnotations(additionalAnnotations)
    override fun withoutAnnotations(): UnionOfStructAndAnyElement = _withoutAnnotations()
    override fun withMetas(additionalMetas: MetaContainer): UnionOfStructAndAnyElement = _withMetas(additionalMetas)
    override fun withMeta(key: String, value: Any): UnionOfStructAndAnyElement = _withMeta(key, value)
    override fun withoutMetas(): UnionOfStructAndAnyElement = _withoutMetas()
}
