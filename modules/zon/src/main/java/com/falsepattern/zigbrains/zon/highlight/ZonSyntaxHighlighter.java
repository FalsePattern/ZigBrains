/*
 * Copyright 2023-2024 FalsePattern
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.falsepattern.zigbrains.zon.highlight;

import com.falsepattern.zigbrains.zon.lexer.ZonLexerAdapter;
import com.falsepattern.zigbrains.zon.psi.ZonTypes;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ZonSyntaxHighlighter extends SyntaxHighlighterBase {
    // @formatter:off
    public static final TextAttributesKey
        EQ       = createKey("EQ"           , DefaultLanguageHighlighterColors.OPERATION_SIGN),
        ID       = createKey("ID"           , DefaultLanguageHighlighterColors.INSTANCE_FIELD),
        COMMENT  = createKey("COMMENT"      , DefaultLanguageHighlighterColors.LINE_COMMENT  ),
        BAD_CHAR = createKey("BAD_CHARACTER", HighlighterColors.BAD_CHARACTER                ),
        STRING   = createKey("STRING"       , DefaultLanguageHighlighterColors.STRING        ),
        COMMA    = createKey("COMMA"        , DefaultLanguageHighlighterColors.COMMA         ),
        DOT      = createKey("DOT"          , DefaultLanguageHighlighterColors.DOT           ),
        BRACE    = createKey("BRACE"        , DefaultLanguageHighlighterColors.BRACES        );
    // @formatter:on

    private static final TextAttributesKey[] EMPTY_KEYS = new TextAttributesKey[0];
    private static final Map<IElementType, TextAttributesKey[]> KEYMAP = new HashMap<>();

    static {
        // @formatter:off
        addMapping(DOT     , ZonTypes.DOT);
        addMapping(COMMA   , ZonTypes.COMMA);
        addMapping(BRACE   , ZonTypes.LBRACE);
        addMapping(BRACE   , ZonTypes.RBRACE);
        addMapping(STRING  , ZonTypes.LINE_STRING, ZonTypes.STRING_LITERAL_SINGLE);
        addMapping(BAD_CHAR, TokenType.BAD_CHARACTER);
        addMapping(COMMENT , ZonTypes.COMMENT);
        addMapping(ID      , ZonTypes.ID);
        addMapping(EQ      , ZonTypes.EQ);
        // @formatter:on
    }

    private static void addMapping(TextAttributesKey key, IElementType... types) {
        for (var type : types) {
            KEYMAP.put(type, new TextAttributesKey[]{key});
        }
    }

    private static TextAttributesKey createKey(String name, TextAttributesKey fallback) {
        return TextAttributesKey.createTextAttributesKey("ZON_" + name, fallback);
    }

    @Override
    public @NotNull Lexer getHighlightingLexer() {
        return new ZonLexerAdapter();
    }

    @Override
    public TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType) {
        if (KEYMAP.containsKey(tokenType)) {
            return KEYMAP.get(tokenType);
        }
        return EMPTY_KEYS;
    }
}
