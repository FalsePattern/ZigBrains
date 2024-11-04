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

package com.falsepattern.zigbrains.zon.highlighting

import com.falsepattern.zigbrains.zon.lexer.ZonLexerAdapter
import com.falsepattern.zigbrains.zon.psi.ZonTypes
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NonNls
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as DefaultColors

class ZonSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer() = ZonLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType?) = KEYMAP.getOrDefault(tokenType, EMPTY_KEYS)

    companion object {
        // @formatter:off
        val EQ       = createKey("EQ"           , DefaultColors.OPERATION_SIGN   )
        val ID       = createKey("ID"           , DefaultColors.INSTANCE_FIELD   )
        val COMMENT  = createKey("COMMENT"      , DefaultColors.LINE_COMMENT     )
        val BAD_CHAR = createKey("BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)
        val STRING   = createKey("STRING"       , DefaultColors.STRING           )
        val COMMA    = createKey("COMMA"        , DefaultColors.COMMA            )
        val BOOLEAN  = createKey("BOOLEAN"      , DefaultColors.KEYWORD          )
        val DOT      = createKey("DOT"          , DefaultColors.DOT              )
        val BRACE    = createKey("BRACE"        , DefaultColors.BRACES           )
        // @formatter:on

        private val EMPTY_KEYS = emptyArray<TextAttributesKey>()
        private val KEYMAP = HashMap<IElementType, Array<TextAttributesKey>>()

        private fun createKey(name: @NonNls String, fallback: TextAttributesKey) =
            TextAttributesKey.createTextAttributesKey("ZON_$name", fallback)

        private fun addMapping(key: TextAttributesKey, vararg types: IElementType) = types.forEach { KEYMAP[it] = arrayOf(key) }

        init {
            // @formatter:off
            addMapping(DOT     , ZonTypes.DOT)
            addMapping(COMMA   , ZonTypes.COMMA)
            addMapping(BRACE   , ZonTypes.LBRACE)
            addMapping(BRACE   , ZonTypes.RBRACE)
            addMapping(STRING  , ZonTypes.LINE_STRING, ZonTypes.STRING_LITERAL_SINGLE)
            addMapping(BAD_CHAR, TokenType.BAD_CHARACTER)
            addMapping(COMMENT , ZonTypes.COMMENT)
            addMapping(ID      , ZonTypes.ID)
            addMapping(EQ      , ZonTypes.EQ)
            addMapping(BOOLEAN , ZonTypes.BOOL_FALSE, ZonTypes.BOOL_TRUE)
            // @formatter:on
        }
    }
}