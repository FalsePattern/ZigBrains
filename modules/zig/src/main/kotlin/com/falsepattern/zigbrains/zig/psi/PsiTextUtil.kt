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

fun CharSequence.getMultiLineContent(startMark: String): List<TextRange> {
    val result = ArrayList<TextRange>()
    val textLength = this.length
    val markLength = startMark.length
    var offset = 0

    while (offset < textLength) {
        val markIndex = this.indexOf(startMark, offset)
        if (markIndex < 0)
            break
        val stringStart = markIndex + markLength

        var found: Int? = null
        findEnd@ for (end in stringStart until textLength) {
            val cI = this[end]
            when (cI) {
                '\r' -> {
                    if (end + 1 < textLength && this[end + 1] == '\n') {
                        found = end + 2
                        break@findEnd
                    }
                    found = end + 1
                    break@findEnd
                }

                '\n' -> {
                    found = end + 1
                    break@findEnd
                }
            }
        }
        if (found == null)
            break

        result.add(TextRange(stringStart, kotlin.math.min(textLength - 1, found)))
        offset = found
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

    val (rangeStart, rangeEnd) = token.textRange
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


operator fun Segment.component1(): Int {
    return startOffset
}

operator fun Segment.component2(): Int {
    return endOffset
}