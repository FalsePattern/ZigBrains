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
import com.redhat.devtools.lsp4ij.features.semanticTokens.SemanticTokensHighlightingColors;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ZigSyntaxHighlighter extends SyntaxHighlighterBase {
    // @formatter:off
    public static final TextAttributesKey
            BAD_CHAR          = createKey("BAD_CHARACTER"      , HighlighterColors.BAD_CHARACTER                      ),
            BUILTIN           = createKey("BUILTIN"            , SemanticTokensHighlightingColors.STATIC_METHOD       ),
            CHAR              = createKey("CHAR"               , SemanticTokensHighlightingColors.NUMBER              ),
            COMMENT           = createKey("COMMENT"            , SemanticTokensHighlightingColors.COMMENT             ),
            COMMENT_DOC       = createKey("COMMENT_DOC"        , DefaultLanguageHighlighterColors.DOC_COMMENT         ),
            ENUM_DECL         = createKey("ENUM_DECL"          , SemanticTokensHighlightingColors.CLASS_DECLARATION   ),
            ENUM_REF          = createKey("ENUM"               , SemanticTokensHighlightingColors.CLASS               ),
            ENUM_MEMBER_DECL  = createKey("ENUM_MEMBER_DECL"   , SemanticTokensHighlightingColors.STATIC_PROPERTY     ),
            ENUM_MEMBER_REF   = createKey("ENUM_MEMBER"        , ENUM_MEMBER_DECL                                     ),
            ERROR_TAG_DECL    = createKey("ERROR_TAG_DECL"     , SemanticTokensHighlightingColors.STATIC_PROPERTY     ),
            ERROR_TAG_REF     = createKey("ERROR_TAG"          , ERROR_TAG_DECL                                       ),
            PROPERTY_DECL     = createKey("PROPERTY_DECL"      , SemanticTokensHighlightingColors.PROPERTY            ),
            PROPERTY_REF      = createKey("PROPERTY"           , PROPERTY_DECL                                        ),
            FUNCTION_DECL     = createKey("FUNCTION_DECL"      , SemanticTokensHighlightingColors.FUNCTION_DECLARATION),
            FUNCTION_DECL_GEN = createKey("FUNCTION_DECL_GEN"  , FUNCTION_DECL                                        ),
            FUNCTION_REF      = createKey("FUNCTION"           , SemanticTokensHighlightingColors.FUNCTION            ),
            FUNCTION_REF_GEN  = createKey("FUNCTION_GEN"       , FUNCTION_REF                                         ),
            KEYWORD           = createKey("KEYWORD"            , SemanticTokensHighlightingColors.KEYWORD             ),
            LABEL_DECL        = createKey("LABEL_DECL"         , SemanticTokensHighlightingColors.LABEL               ),
            LABEL_REF         = createKey("LABEL"              , LABEL_DECL                                           ),
            METHOD_DECL       = createKey("METHOD_DECL"        , FUNCTION_DECL                                        ),
            METHOD_DECL_GEN   = createKey("METHOD_DECL_GEN"    , METHOD_DECL                                          ),
            METHOD_REF        = createKey("METHOD"             , FUNCTION_REF                                         ),
            METHOD_REF_GEN    = createKey("METHOD_GEN"         , METHOD_REF                                           ),
            NAMESPACE_DECL    = createKey("NAMESPACE_DECL"     , SemanticTokensHighlightingColors.CLASS_DECLARATION   ),
            NAMESPACE_REF     = createKey("NAMESPACE"          , SemanticTokensHighlightingColors.CLASS               ),
            NUMBER            = createKey("NUMBER"             , SemanticTokensHighlightingColors.NUMBER              ),
            OPERATOR          = createKey("OPERATOR"           , SemanticTokensHighlightingColors.OPERATOR            ),
            PARAMETER         = createKey("PARAMETER"          , SemanticTokensHighlightingColors.PARAMETER           ),
            STRING            = createKey("STRING"             , SemanticTokensHighlightingColors.STRING              ),
            STRUCT_DECL       = createKey("STRUCT_DECL"        , SemanticTokensHighlightingColors.CLASS_DECLARATION   ),
            STRUCT_REF        = createKey("STRUCT"             , SemanticTokensHighlightingColors.CLASS               ),
            TYPE_DECL         = createKey("TYPE_DECL"          , SemanticTokensHighlightingColors.CLASS_DECLARATION   ),
            TYPE_DECL_GEN     = createKey("TYPE_DECL_GEN"      , TYPE_DECL                                            ),
            TYPE_REF          = createKey("TYPE"               , SemanticTokensHighlightingColors.TYPE                ),
            TYPE_REF_GEN      = createKey("TYPE_GEN"           , TYPE_REF                                             ),
            TYPE_PARAM        = createKey("TYPE_PARAM"         , SemanticTokensHighlightingColors.TYPE_PARAMETER      ),
            TYPE_PARAM_DECL   = createKey("TYPE_PARAM_DECL"    , TYPE_PARAM                                           ),
            VARIABLE_DECL     = createKey("VARIABLE_DECL"      , DefaultLanguageHighlighterColors.LOCAL_VARIABLE      ),
            VARIABLE_DECL_DEPR= createKey("VARIABLE_DECL_DEPR" , VARIABLE_DECL                                        ),
            VARIABLE_REF      = createKey("VARIABLE"           , VARIABLE_DECL                                        ),
            VARIABLE_REF_DEPR = createKey("VARIABLE_REF_DEPL"  , VARIABLE_REF                                         );
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
