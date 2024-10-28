package com.falsepattern.zigbrains.zig.parser

import com.falsepattern.zigbrains.zig.psi.ZigTypes
import com.intellij.psi.tree.TokenSet

object ZigTokenSets {
    val COMMENTS = TokenSet.create(ZigTypes.LINE_COMMENT, ZigTypes.DOC_COMMENT, ZigTypes.CONTAINER_DOC_COMMENT)
    val STRINGS = TokenSet.create(ZigTypes.STRING_LITERAL_SINGLE, ZigTypes.STRING_LITERAL_MULTI)
}