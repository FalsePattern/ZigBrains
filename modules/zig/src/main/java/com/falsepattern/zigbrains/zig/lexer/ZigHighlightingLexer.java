package com.falsepattern.zigbrains.zig.lexer;

import com.falsepattern.zigbrains.zig.psi.ZigTypes;
import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.LayeredLexer;
import com.intellij.lexer.MergingLexerAdapter;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import lombok.val;

public class ZigHighlightingLexer extends LayeredLexer {
    public ZigHighlightingLexer() {
        super(new ZigLexerAdapter());
        val stringLexer = new MergingLexerAdapter(new FlexAdapter(new com.falsepattern.zigbrains.zig.stringlexer.ZigStringLexer(null)), TokenSet.create(
                ZigTypes.STRING_LITERAL_SINGLE));
        registerSelfStoppingLayer(stringLexer, new IElementType[]{ZigTypes.STRING_LITERAL_SINGLE}, IElementType.EMPTY_ARRAY);
    }
}
