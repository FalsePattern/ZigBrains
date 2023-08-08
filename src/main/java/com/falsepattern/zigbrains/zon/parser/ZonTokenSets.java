package com.falsepattern.zigbrains.zon.parser;

import com.falsepattern.zigbrains.zon.psi.ZonTypes;
import com.intellij.psi.tree.TokenSet;

public interface ZonTokenSets {
    TokenSet COMMENTS = TokenSet.create(ZonTypes.COMMENT);
    TokenSet STRINGS = TokenSet.create(ZonTypes.LINE_STRING, ZonTypes.STRING_LITERAL_SINGLE);
}
