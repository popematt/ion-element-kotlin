package com.amazon.ionelement.encoding.text

import com.amazon.ion.IonType
import com.amazon.ion.IonType.*
import com.amazon.ionelement.api.AnyElement
import com.amazon.ionelement.api.ElementType
import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionListOf
import com.amazon.ionelement.api.ionNull
import com.amazon.ionelement.api.ionString

sealed class IonTextTree {
    fun traverseTokens(): Sequence<IonTextToken> {
        return when (this) {
            is Value -> children.asSequence().flatMap { it.traverseTokens() }
            is Field -> children.asSequence().flatMap { it.traverseTokens() }
            is Annotation -> sequenceOf(token)
            is FieldName -> sequenceOf(token)
            is Token -> sequenceOf(token)
            is SyntaxError -> children.asSequence().flatMap { it.traverseTokens() }
        }
    }
    fun toSexpString(depth: Int = 0): String {
        val lineStart = "\n" + "  ".repeat(depth)
        return lineStart + when (this) {
            is Value -> "(${type.toString().toLowerCase()} ${children.joinToString(" ") { it.toSexpString(depth + 1) }})"
            is Field -> "(struct_field ${children.joinToString(" ") { it.toSexpString(depth + 1) }})"
            is FieldName -> "(field_name ${token.toSexpString()})"
            is Annotation -> "(annotation ${token.toSexpString()})"
            is Token -> token.toSexpString()
            is SyntaxError -> "(syntax-error \"$message\" ${children.joinToString(" ") { it.toSexpString(depth + 1) }})"
        }
    }

    data class Value(val type: IonType, val children: List<IonTextTree>): IonTextTree() {
        val typeAnnotationStrings: List<String>
            get() = children.filterIsInstance<Annotation>().map { it.token.content.trimQuotes("'").toString() }
    }

    data class Field(val children: List<IonTextTree>): IonTextTree() {
        val name: String
            get() = (children.first() as FieldName).token.content.trim('\'').toString()
        val value: Value
            get() = children.last() as Value
    }

    data class FieldName(val token: IonTextToken): IonTextTree()

    data class Annotation(val token: IonTextToken): IonTextTree()

    data class Token(val token: IonTextToken): IonTextTree()

    data class SyntaxError(val message: String, val children: List<IonTextTree>): IonTextTree() {
        constructor(message: String, child: IonTextTree): this(message, listOf(child))
    }
}

fun IonTextTree.getSyntaxErrors(): List<IonTextTree.SyntaxError> {
    return when (this) {
        is IonTextTree.Annotation,
        is IonTextTree.FieldName,
        is IonTextTree.Token -> emptyList()
        is IonTextTree.Field -> children.flatMap { it.getSyntaxErrors() }
        is IonTextTree.Value -> children.flatMap { it.getSyntaxErrors() }
        is IonTextTree.SyntaxError -> listOf(this) + children.flatMap { it.getSyntaxErrors() }
    }
}

fun List<IonTextTree>.toAnyElement(): AnyElement = filterIsInstance<IonTextTree.Value>().single().toAnyElement()

fun IonTextTree.Value.toAnyElement(): AnyElement {
    val metas = children.first().let {
        if (it is IonTextTree.Token) it.token.metas
        else if (it is IonTextTree.Annotation) it.token.metas
        else TODO("Unreachable")
    }
    val annotations = children.filterIsInstance<IonTextTree.Annotation>()
        .map { it.token.content.trim('\'').toString() }
    return when (type) {
        STRING -> ionString(
            s = children.asSequence().filterIsInstance<IonTextTree.Token>()
                .filter { it.token.type is TokenType.PartialValue }
                .map { it.token.content }
                .map { if (it.startsWith("'''")) it.trim('\'') else it.trim('"') }
                .joinToString(""),
            annotations = annotations,
            metas = metas
        )
        NULL -> TODO()
        BOOL -> ionBool(
            b = children.last().let { it as IonTextTree.Token }.token.content == "true",
            annotations = annotations,
            metas = metas
        )
        INT -> TODO()
        FLOAT -> TODO()
        DECIMAL -> TODO()
        TIMESTAMP -> TODO()
        SYMBOL -> TODO()
        CLOB -> TODO()
        BLOB -> TODO()
        LIST -> ionListOf(
            iterable = children.filterIsInstance<IonTextTree.Value>().map { it.toAnyElement() },
            annotations = annotations,
            metas = metas
        )
        SEXP -> TODO()
        STRUCT -> TODO()
        DATAGRAM -> TODO("Intentionally Not implemented.")
    }.asAnyElement()
}

