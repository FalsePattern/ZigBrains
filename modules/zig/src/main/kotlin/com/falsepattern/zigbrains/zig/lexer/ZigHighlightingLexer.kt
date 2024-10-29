package com.falsepattern.zigbrains.zig.lexer

import com.falsepattern.zigbrains.zig.lexerstring.ZigLexerStringAdapter
import com.falsepattern.zigbrains.zig.psi.ZigTypes
import com.intellij.lexer.LayeredLexer
import com.intellij.lexer.MergingLexerAdapter
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

class ZigHighlightingLexer: LayeredLexer(ZigLexerAdapter()) {
    init {
        registerSelfStoppingLayer(
            MergingLexerAdapter(
                ZigLexerStringAdapter(),
                TokenSet.create(ZigTypes.STRING_LITERAL_SINGLE)
            ),
            arrayOf(ZigTypes.STRING_LITERAL_SINGLE),
            IElementType.EMPTY_ARRAY
        )
    }
}