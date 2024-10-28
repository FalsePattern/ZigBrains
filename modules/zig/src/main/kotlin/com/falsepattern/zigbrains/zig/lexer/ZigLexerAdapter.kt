package com.falsepattern.zigbrains.zig.lexer

import com.intellij.lexer.FlexAdapter

class ZigLexerAdapter: FlexAdapter(ZigFlexLexer(null))