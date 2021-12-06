package com.amazon.ionelement.encoding.text

sealed class TokenType {
    object Comment: TokenType() { override fun toString(): String = this.javaClass.simpleName }
    object WhiteSpace: TokenType() { override fun toString(): String = this.javaClass.simpleName }
    object Delimiter: TokenType() { override fun toString(): String = this.javaClass.simpleName }
    object PartialValue: TokenType() { override fun toString(): String = this.javaClass.simpleName }
    object NotIon: TokenType() { override fun toString(): String = this.javaClass.simpleName }
}
