package com.falsepattern.zigbrains.zon.pairing

import com.falsepattern.zigbrains.zon.psi.ZonTypes
import com.intellij.codeInsight.editorActions.MultiCharQuoteHandler
import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler
import com.intellij.openapi.editor.highlighter.HighlighterIterator

class ZonQuoteHandler: SimpleTokenSetQuoteHandler(ZonTypes.STRING_LITERAL, ZonTypes.BAD_STRING), MultiCharQuoteHandler {
    override fun getClosingQuote(iterator: HighlighterIterator, offset: Int) =
        "\""
}