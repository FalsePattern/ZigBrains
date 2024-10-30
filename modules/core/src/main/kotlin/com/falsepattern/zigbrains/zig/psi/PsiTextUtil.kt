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

package com.falsepattern.zigbrains.zig.psi

import com.falsepattern.zigbrains.zig.lexerstring.ZigLexerStringAdapter
import com.falsepattern.zigbrains.zig.util.prefixWithTextBlockEscape
import com.falsepattern.zigbrains.zig.util.unescape
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Segment
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.StringEscapesTokenTypes
import com.intellij.util.MathUtil
import kotlin.math.max


fun getTextRangeBounds(contentRanges: List<TextRange>): TextRange =
    if (contentRanges.isEmpty())
        TextRange.EMPTY_RANGE
    else
        TextRange.create(contentRanges.first().startOffset, contentRanges.last().endOffset)

fun CharSequence.getMultilineContent(startMark: String): List<TextRange> {
    val result = ArrayList<TextRange>()
    var stringStart = 0
    var inBody = false
    val textLength = length
    val firstChar = startMark[0]
    val extraChars = startMark.substring(1)
    var i = 0
    loop@ while (i < textLength) {
        val cI = this[i]
        if (!inBody) {
            if (cI == firstChar &&
                i + extraChars.length < textLength
            ) {
                for (j in extraChars.indices) {
                    if (this[i + j + 1] != startMark[j]) {
                        i++
                        continue@loop
                    }
                }
                i += extraChars.length
                inBody = true
                stringStart = i + 1
            }
        } else if (cI == '\r') {
            if (i + 1 < length && this[i + 1] == '\n') {
                i++
            }
            inBody = false
            result.add(
                TextRange(
                    stringStart,
                    kotlin.math.min((textLength - 1), (i + 1))
                )
            )
        } else if (cI == '\n') {
            inBody = false
            result.add(
                TextRange(
                    stringStart,
                    kotlin.math.min((textLength - 1), (i + 1))
                )
            )
        }
        i++
    }
    return result
}

val PsiElement.indentSize: Int get() = StringUtil.offsetToLineColumn(containingFile.text, textOffset)?.column ?: 0

fun splitString(
    editor: Editor,
    psiAtOffset: PsiElement,
    caretOffset: Int,
    insertNewlineAtCaret: Boolean
) {
    @Suppress("NAME_SHADOWING")
    var caretOffset = caretOffset
    val document = editor.document
    val token = psiAtOffset.node
    val text = document.charsSequence

    val range = token.textRange
    val rangeStart = range.startOffset
    val rangeEnd = range.endOffset
    val lexer = ZigLexerStringAdapter()
    lexer.start(text, rangeStart, rangeEnd)
    caretOffset = lexer.skipStringLiteralEscapes(caretOffset)
    caretOffset = MathUtil.clamp(caretOffset, rangeStart + 1, rangeEnd - 1)
    val unescapedPrefix = text.subSequence(rangeStart + 1, caretOffset).unescape(false)
    val unescapedSuffix = text.subSequence(caretOffset, rangeEnd - 1).unescape(false)
    val stringRange = document.createRangeMarker(rangeStart, rangeEnd)
    stringRange.isGreedyToRight = true
    val lineNumber = document.getLineNumber(caretOffset)
    val lineOffset = document.getLineStartOffset(lineNumber)
    val indent = stringRange.startOffset - lineOffset
    val lineIndent = StringUtil.skipWhitespaceForward(document.getText(TextRange(lineOffset, stringRange.startOffset)), 0)
    val newLine = indent != lineIndent
    val finalIndent = if (newLine) lineIndent + 4 else lineIndent
    document.deleteString(stringRange.startOffset, stringRange.endOffset)
    document.insertString(
        stringRange.startOffset,
        unescapedPrefix.prefixWithTextBlockEscape(
            indent = finalIndent,
            marker = "\\\\",
            indentFirst = newLine,
            prefixFirst = true,
            newLineAfter = insertNewlineAtCaret
        )
    )
    caretOffset = stringRange.endOffset
    document.insertString(
        caretOffset,
        unescapedSuffix.prefixWithTextBlockEscape(
            indent = finalIndent,
            marker = "\\\\",
            indentFirst = false,
            prefixFirst = false,
            newLineAfter = false
        )
    )
    var end = stringRange.endOffset
    while (end < text.length) {
        when (text[end]) {
            ' ', '\t' -> {
                end++
                continue
            }
            else -> break
        }
    }
    document.replaceString(
        stringRange.endOffset,
        end,
        "\n" + " ".repeat(if (newLine) lineIndent else max(lineIndent - 4, 0))
    )
    stringRange.dispose()
    editor.caretModel.moveToOffset(caretOffset)
}

private fun Lexer.skipStringLiteralEscapes(caretOffset: Int): Int {
    while (tokenType != null) {
        if (caretOffset in (tokenStart + 1)..<tokenEnd) {
            if (StringEscapesTokenTypes.STRING_LITERAL_ESCAPES.contains(tokenType)) {
                return tokenEnd
            }
            return caretOffset
        }
        advance()
    }
    return caretOffset
}