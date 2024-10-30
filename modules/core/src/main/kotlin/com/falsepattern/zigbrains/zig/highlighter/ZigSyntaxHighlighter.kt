/*
 * This file is part of ZigBrains.
 *
 * Copyright (C) 2023-2024 FalsePattern
 * All Rights Reserved
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * ZigBrains is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 *
 * ZigBrains is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ZigBrains. If not, see <https://www.gnu.org/licenses/>.
 */

package com.falsepattern.zigbrains.zig.highlighter

import com.falsepattern.zigbrains.zig.lexer.ZigHighlightingLexer
import com.falsepattern.zigbrains.zig.psi.ZigTypes
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.StringEscapesTokenTypes
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

class ZigSyntaxHighlighter: SyntaxHighlighterBase() {
    override fun getHighlightingLexer() = ZigHighlightingLexer()

    override fun getTokenHighlights(tokenType: IElementType?) =
        KEYMAP.getOrDefault(tokenType, EMPTY_KEYS)

    companion object {

        // @formatter:off
        val BAD_CHAR          = createKey("BAD_CHARACTER"      , HighlighterColors.BAD_CHARACTER                        )
        val BUILTIN           = createKey("BUILTIN"            , DefaultLanguageHighlighterColors.STATIC_METHOD         )
        val CHAR              = createKey("CHAR"               , DefaultLanguageHighlighterColors.NUMBER                )
        val COMMENT           = createKey("COMMENT"            , DefaultLanguageHighlighterColors.LINE_COMMENT          )
        val COMMENT_DOC       = createKey("COMMENT_DOC"        , DefaultLanguageHighlighterColors.DOC_COMMENT           )
        val ENUM_DECL         = createKey("ENUM_DECL"          , DefaultLanguageHighlighterColors.CLASS_NAME            )
        val ENUM_REF          = createKey("ENUM"               , DefaultLanguageHighlighterColors.CLASS_REFERENCE       )
        val ENUM_MEMBER_DECL  = createKey("ENUM_MEMBER_DECL"   , DefaultLanguageHighlighterColors.STATIC_FIELD          )
        val ENUM_MEMBER_REF   = createKey("ENUM_MEMBER"        , ENUM_MEMBER_DECL                                       )
        val ERROR_TAG_DECL    = createKey("ERROR_TAG_DECL"     , DefaultLanguageHighlighterColors.STATIC_FIELD          )
        val ERROR_TAG_REF     = createKey("ERROR_TAG"          , ERROR_TAG_DECL                                         )
        val PROPERTY_DECL     = createKey("PROPERTY_DECL"      , DefaultLanguageHighlighterColors.STATIC_FIELD          )
        val PROPERTY_REF      = createKey("PROPERTY"           , PROPERTY_DECL                                          )
        val FUNCTION_DECL     = createKey("FUNCTION_DECL"      , DefaultLanguageHighlighterColors.FUNCTION_DECLARATION  )
        val FUNCTION_DECL_GEN = createKey("FUNCTION_DECL_GEN"  , FUNCTION_DECL                                          )
        val FUNCTION_REF      = createKey("FUNCTION"           , DefaultLanguageHighlighterColors.FUNCTION_CALL         )
        val FUNCTION_REF_GEN  = createKey("FUNCTION_GEN"       , FUNCTION_REF                                           )
        val KEYWORD           = createKey("KEYWORD"            , DefaultLanguageHighlighterColors.KEYWORD               )
        val LABEL_DECL        = createKey("LABEL_DECL"         , DefaultLanguageHighlighterColors.LABEL                 )
        val LABEL_REF         = createKey("LABEL"              , LABEL_DECL                                             )
        val METHOD_DECL       = createKey("METHOD_DECL"        , FUNCTION_DECL                                          )
        val METHOD_DECL_GEN   = createKey("METHOD_DECL_GEN"    , METHOD_DECL                                            )
        val METHOD_REF        = createKey("METHOD"             , FUNCTION_REF                                           )
        val METHOD_REF_GEN    = createKey("METHOD_GEN"         , METHOD_REF                                             )
        val NAMESPACE_DECL    = createKey("NAMESPACE_DECL"     , DefaultLanguageHighlighterColors.CLASS_NAME            )
        val NAMESPACE_REF     = createKey("NAMESPACE"          , DefaultLanguageHighlighterColors.CLASS_REFERENCE       )
        val NUMBER            = createKey("NUMBER"             , DefaultLanguageHighlighterColors.NUMBER                )
        val OPERATOR          = createKey("OPERATOR"           , DefaultLanguageHighlighterColors.OPERATION_SIGN        )
        val PARAMETER         = createKey("PARAMETER"          , DefaultLanguageHighlighterColors.PARAMETER             )
        val STRING            = createKey("STRING"             , DefaultLanguageHighlighterColors.STRING                )
        val STRING_ESC_V      = createKey("STRING_ESC_V"       , DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE   )
        val STRING_ESC_I_C    = createKey("STRING_ESC_I_C"     , DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE )
        val STRING_ESC_I_U    = createKey("STRING_ESC_I_U"     , DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE )
        val STRUCT_DECL       = createKey("STRUCT_DECL"        , DefaultLanguageHighlighterColors.CLASS_NAME            )
        val STRUCT_REF        = createKey("STRUCT"             , DefaultLanguageHighlighterColors.CLASS_REFERENCE       )
        val TYPE_DECL         = createKey("TYPE_DECL"          , DefaultLanguageHighlighterColors.CLASS_NAME            )
        val TYPE_DECL_GEN     = createKey("TYPE_DECL_GEN"      , TYPE_DECL                                              )
        val TYPE_REF          = createKey("TYPE"               , DefaultLanguageHighlighterColors.CLASS_REFERENCE       )
        val TYPE_REF_GEN      = createKey("TYPE_GEN"           , TYPE_REF                                               )
        val TYPE_PARAM        = createKey("TYPE_PARAM"         , DefaultLanguageHighlighterColors.PARAMETER             )
        val TYPE_PARAM_DECL   = createKey("TYPE_PARAM_DECL"    , TYPE_PARAM                                             )
        val VARIABLE_DECL     = createKey("VARIABLE_DECL"      , DefaultLanguageHighlighterColors.LOCAL_VARIABLE        )
        val VARIABLE_DECL_DEPR= createKey("VARIABLE_DECL_DEPR" , VARIABLE_DECL                                          )
        val VARIABLE_REF      = createKey("VARIABLE"           , VARIABLE_DECL                                          )
        val VARIABLE_REF_DEPR = createKey("VARIABLE_REF_DEPL"  , VARIABLE_REF                                           )
        // @formatter:on

        private val EMPTY_KEYS = arrayOf<TextAttributesKey>()
        private val KEYMAP = HashMap<IElementType, Array<TextAttributesKey>>()

        init {
            addMapping(COMMENT, ZigTypes.LINE_COMMENT)
            addMapping(COMMENT_DOC, ZigTypes.DOC_COMMENT, ZigTypes.CONTAINER_DOC_COMMENT)

            addMapping(
                KEYWORD,
                *ZigTypes::class.java
                    .fields
                    .filter { it.name.startsWith("KEYWORD_") }
                    .map { it.get(null) as IElementType }
                    .toTypedArray()
            )
            addMapping(BUILTIN, ZigTypes.BUILTINIDENTIFIER)
            addMapping(STRING, ZigTypes.STRING_LITERAL_SINGLE, ZigTypes.STRING_LITERAL_MULTI)
            addMapping(STRING_ESC_V, StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN)
            addMapping(STRING_ESC_I_C, StringEscapesTokenTypes.INVALID_CHARACTER_ESCAPE_TOKEN)
            addMapping(STRING_ESC_I_U, StringEscapesTokenTypes.INVALID_UNICODE_ESCAPE_TOKEN)
            addMapping(BAD_CHAR, TokenType.BAD_CHARACTER)
            addMapping(NUMBER, ZigTypes.INTEGER, ZigTypes.FLOAT)
            addMapping(CHAR, ZigTypes.CHAR_LITERAL)
        }

        private fun addMapping(key: TextAttributesKey, vararg types: IElementType) {
            val a = arrayOf(key)
            for (type in types) {
                KEYMAP[type] = a
            }
        }

        private fun createKey(name: String, fallback: TextAttributesKey) =
            TextAttributesKey.createTextAttributesKey("ZIG_$name", fallback)
    }
}