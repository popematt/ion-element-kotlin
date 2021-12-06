package com.amazon.ionelement.encoding.text

import com.amazon.ionelement.api.MetaContainer

abstract class IonTextToken {
    abstract val content: CharSequence
    abstract val type: TokenType
    abstract val metas: MetaContainer

    fun toSexpString(): String = if (type is TokenType.PartialValue)
            "(token ${type.toString().toLowerCase()} $content)".replace("\n", "\\n")
        else
            "(token ${type.toString().toLowerCase()} \"$content\")".replace("\n", "\\n")
}
