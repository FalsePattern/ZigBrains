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

package com.falsepattern.zigbrains.zig.highlighter;

import com.falsepattern.zigbrains.zig.lexer.ZigLexerAdapter;
import com.falsepattern.zigbrains.zig.psi.ZigTypes;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ZigSyntaxHighlighter extends SyntaxHighlighterBase {
    // @formatter:off
    public static final TextAttributesKey
            BAD_CHAR          = createKey("BAD_CHARACTER"      , HighlighterColors.BAD_CHARACTER                      ),
            BUILTIN           = createKey("BUILTIN"            , DefaultLanguageHighlighterColors.STATIC_METHOD       ),
            CHAR              = createKey("CHAR"               , DefaultLanguageHighlighterColors.NUMBER              ),
            COMMENT           = createKey("COMMENT"            , DefaultLanguageHighlighterColors.LINE_COMMENT        ),
            COMMENT_DOC       = createKey("COMMENT_DOC"        , DefaultLanguageHighlighterColors.DOC_COMMENT         ),
            ENUM_DECL         = createKey("ENUM_DECL"          , DefaultLanguageHighlighterColors.CLASS_NAME          ),
            ENUM_REF          = createKey("ENUM"               , DefaultLanguageHighlighterColors.CLASS_REFERENCE     ),
            ENUM_MEMBER       = createKey("ENUM_MEMBER"        , DefaultLanguageHighlighterColors.STATIC_FIELD        ),
            ERROR_TAG         = createKey("ERROR_TAG"          , DefaultLanguageHighlighterColors.STATIC_FIELD        ),
            PROPERTY          = createKey("PROPERTY"           , DefaultLanguageHighlighterColors.INSTANCE_FIELD      ),
            FUNCTION_DECL     = createKey("FUNCTION_DECL"      , DefaultLanguageHighlighterColors.FUNCTION_DECLARATION),
            FUNCTION_DECL_GEN = createKey("FUNCTION_DECL_GEN"  , FUNCTION_DECL                                        ),
            FUNCTION_REF      = createKey("FUNCTION"           , DefaultLanguageHighlighterColors.FUNCTION_CALL       ),
            FUNCTION_REF_GEN  = createKey("FUNCTION_GEN"       , FUNCTION_REF                                         ),
            KEYWORD           = createKey("KEYWORD"            , DefaultLanguageHighlighterColors.KEYWORD             ),
            LABEL_DECL        = createKey("LABEL_DECL"         , DefaultLanguageHighlighterColors.LABEL               ),
            LABEL_REF         = createKey("LABEL"              , LABEL_DECL                                           ),
            NAMESPACE_DECL    = createKey("NAMESPACE_DECL"     , DefaultLanguageHighlighterColors.CLASS_REFERENCE     ),
            NAMESPACE_REF     = createKey("NAMESPACE"          , DefaultLanguageHighlighterColors.CLASS_NAME          ),
            NUMBER            = createKey("NUMBER"             , DefaultLanguageHighlighterColors.NUMBER              ),
            OPERATOR          = createKey("OPERATOR"           , DefaultLanguageHighlighterColors.OPERATION_SIGN      ),
            PARAMETER         = createKey("PARAMETER"          , DefaultLanguageHighlighterColors.PARAMETER           ),
            STRING            = createKey("STRING"             , DefaultLanguageHighlighterColors.STRING              ),
            STRUCT_DECL       = createKey("STRUCT_DECL"        , DefaultLanguageHighlighterColors.CLASS_NAME          ),
            STRUCT_REF        = createKey("STRUCT"             , DefaultLanguageHighlighterColors.CLASS_REFERENCE     ),
            TYPE_DECL         = createKey("TYPE_DECL"          , DefaultLanguageHighlighterColors.CLASS_NAME          ),
            TYPE_DECL_GEN     = createKey("TYPE_DECL_GEN"      , TYPE_DECL                                            ),
            TYPE_REF          = createKey("TYPE"               , DefaultLanguageHighlighterColors.CLASS_REFERENCE     ),
            TYPE_REF_GEN      = createKey("TYPE_GEN"           , TYPE_REF                                             ),
            VARIABLE_DECL     = createKey("VARIABLE_DECL"      , DefaultLanguageHighlighterColors.LOCAL_VARIABLE      ),
            VARIABLE_REF      = createKey("VARIABLE"           , VARIABLE_DECL                                        );
    // @formatter:on
    private static final TextAttributesKey[] EMPTY_KEYS = new TextAttributesKey[0];
    private static final Map<IElementType, TextAttributesKey[]> KEYMAP = new HashMap<>();

    static {
        // @formatter:off
        addMapping(COMMENT, ZigTypes.LINE_COMMENT, ZigTypes.DOC_COMMENT, ZigTypes.CONTAINER_DOC_COMMENT);

        //Keywords
        {
            var kws = new ArrayList<IElementType>();
            for (var field: ZigTypes.class.getFields()) {
                try {
                    if (field.getName().startsWith("KEYWORD_")) {
                        kws.add((IElementType) field.get(null));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            addMapping(KEYWORD, kws.toArray(IElementType[]::new));
        }
        addMapping(BUILTIN, ZigTypes.BUILTINIDENTIFIER);
        addMapping(STRING, ZigTypes.STRING_LITERAL_SINGLE, ZigTypes.STRING_LITERAL_MULTI);
        addMapping(BAD_CHAR, TokenType.BAD_CHARACTER);
        addMapping(NUMBER, ZigTypes.INTEGER, ZigTypes.FLOAT);
        addMapping(CHAR, ZigTypes.CHAR_LITERAL);
        // @formatter:on
    }

    private static void addMapping(TextAttributesKey key, IElementType... types) {
        for (var type : types) {
            KEYMAP.put(type, new TextAttributesKey[]{key});
        }
    }

    private static TextAttributesKey createKey(String name, TextAttributesKey fallback) {
        return TextAttributesKey.createTextAttributesKey("ZIG_" + name, fallback);
    }

    @Override
    public @NotNull Lexer getHighlightingLexer() {
        return new ZigLexerAdapter();
    }

    @Override
    public TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType) {
        if (KEYMAP.containsKey(tokenType)) {
            return KEYMAP.get(tokenType);
        }
        return EMPTY_KEYS;
    }
}
