package com.amazon.ionelement.encoding.text

import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.emptyMetaContainer

data class SimpleToken(
    override val content: String,
    override val type: TokenType,
    override val metas: MetaContainer = emptyMetaContainer()
): IonTextToken() {
    constructor(content: Char, type: TokenType): this(content.toString(), type)
}
