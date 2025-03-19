/*
 * This file is part of ZigBrains.
 *
 * Copyright (C) 2023-2025 FalsePattern
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

import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.BAD_CHAR
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.CHAR
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.COMMENT
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.KEYWORD
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.NUMBER
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.STRING
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.STRING_ESC_I_C
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.STRING_ESC_I_U
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.STRING_ESC_V
import com.falsepattern.zigbrains.zig.psi.ZigTypes
import com.falsepattern.zigbrains.zon.lexer.ZonHighlightingLexer
import com.falsepattern.zigbrains.zon.psi.ZonTypes
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.StringEscapesTokenTypes
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

class ZonSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer() = ZonHighlightingLexer()

    override fun getTokenHighlights(tokenType: IElementType?) = KEYMAP.getOrDefault(tokenType, EMPTY_KEYS)

    companion object {
        private val EMPTY_KEYS = emptyArray<TextAttributesKey>()
        private val KEYMAP = HashMap<IElementType, Array<TextAttributesKey>>()

        private fun addMapping(key: TextAttributesKey, vararg types: IElementType) = types.forEach { KEYMAP[it] = arrayOf(key) }

        init {
            // @formatter:off
            addMapping(COMMENT, ZonTypes.LINE_COMMENT)
            addMapping(
                KEYWORD,
                *ZonTypes::class.java
                    .fields
                    .filter {
                        it.name.startsWith("KEYWORD_")
                    }
                    .map { it.get(null) as IElementType }
                    .toTypedArray()
            )
            addMapping(STRING, ZonTypes.STRING_LITERAL_SINGLE, ZonTypes.STRING_LITERAL_MULTI, ZigTypes.STRING_LITERAL_SINGLE, ZigTypes.STRING_LITERAL_MULTI)
            addMapping(STRING_ESC_V, StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN)
            addMapping(STRING_ESC_I_C, StringEscapesTokenTypes.INVALID_CHARACTER_ESCAPE_TOKEN)
            addMapping(STRING_ESC_I_U, StringEscapesTokenTypes.INVALID_UNICODE_ESCAPE_TOKEN)
            addMapping(BAD_CHAR, TokenType.BAD_CHARACTER)
            addMapping(NUMBER, ZonTypes.INTEGER, ZonTypes.FLOAT, ZonTypes.NUM_NAN, ZonTypes.NUM_INF)
            addMapping(CHAR, ZonTypes.CHAR_LITERAL, ZigTypes.CHAR_LITERAL)

            // @formatter:on
        }
    }
}