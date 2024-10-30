package com.falsepattern.zigbrains.zig.pairing

import com.falsepattern.zigbrains.zig.psi.ZigTypes
import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType

class ZigBraceMatcher: PairedBraceMatcher {
    override fun getPairs() =
        PAIRS

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?) =
        true

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int) =
        file?.findElementAt(openingBraceOffset)?.parent?.textOffset ?: openingBraceOffset
}

private val BRACE_PAIR = BracePair(ZigTypes.LBRACE, ZigTypes.RBRACE, true)
private val PAREN_PAIR = BracePair(ZigTypes.LPAREN, ZigTypes.RPAREN, false)
private val BRACKET_PAIR = BracePair(ZigTypes.LBRACKET, ZigTypes.RBRACKET, false)
private val PAIRS = arrayOf(BRACE_PAIR, PAREN_PAIR, BRACKET_PAIR)