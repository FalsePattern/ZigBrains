package com.falsepattern.zigbrains.zon.lexer;

import com.intellij.lexer.FlexAdapter;

public class ZonLexerAdapter extends FlexAdapter {
    public ZonLexerAdapter() {
        super(new ZonFlexLexer(null));
    }
}
