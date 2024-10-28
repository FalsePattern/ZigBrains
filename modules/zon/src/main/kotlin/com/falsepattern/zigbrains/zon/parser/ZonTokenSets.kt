package com.falsepattern.zigbrains.zon.parser

import com.falsepattern.zigbrains.zon.psi.ZonTypes
import com.intellij.psi.tree.TokenSet

object ZonTokenSets {
    val COMMENTS = TokenSet.create(ZonTypes.COMMENT)
    val STRINGS = TokenSet.create(ZonTypes.LINE_STRING, ZonTypes.STRING_LITERAL_SINGLE)
}