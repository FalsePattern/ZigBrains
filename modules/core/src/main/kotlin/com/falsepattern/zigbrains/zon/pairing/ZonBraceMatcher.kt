package com.falsepattern.zigbrains.zon.pairing

import com.falsepattern.zigbrains.zon.psi.ZonTypes
import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType

class ZonBraceMatcher : PairedBraceMatcher {
    override fun getPairs() =
        PAIRS

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?) =
        true

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int) =
        file?.findElementAt(openingBraceOffset)?.parent?.textOffset ?: openingBraceOffset
}

private val PAIR = BracePair(ZonTypes.LBRACE, ZonTypes.RBRACE, true)
private val PAIRS = arrayOf(PAIR)