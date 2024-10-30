package com.falsepattern.zigbrains.zon.lexer

import com.intellij.lexer.FlexAdapter

class ZonLexerAdapter: FlexAdapter(ZonFlexLexer(null))