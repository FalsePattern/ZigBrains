package com.falsepattern.zigbrains.zig.intentions

import com.falsepattern.zigbrains.zig.psi.ZigStringLiteral
import com.falsepattern.zigbrains.zig.util.escape
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import kotlin.math.max
import kotlin.math.min

class MakeStringQuoted: PsiElementBaseIntentionAction() {
    init {
        text = familyName
    }
    override fun getFamilyName() = "Convert to quoted"

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement) =
        editor != null && element.parentOfType<ZigStringLiteral>()?.isMultiline ?: false

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        editor ?: return
        val document = editor.document
        val str = element.parentOfType<ZigStringLiteral>() ?: return
        if (!str.isMultiline)
            return
        val escaper = str.createLiteralTextEscaper()
        val contentRange = escaper.relevantTextRange
        val contentStart = contentRange.startOffset
        val contentEnd = contentRange.endOffset
        val fullRange = str.textRange
        val fullStart = fullRange.startOffset
        val fullEnd = fullRange.endOffset
        var caretOffset = editor.caretModel.offset
        val prefix = TextRange(contentStart, max(contentStart, caretOffset - fullStart))
        val suffix = TextRange(min(contentEnd, caretOffset - fullStart), contentEnd)
        val sb = StringBuilder()
        escaper.decode(prefix, sb)
        val prefixStr = sb.escape()
        sb.setLength(0)
        escaper.decode(suffix, sb)
        val suffixStr = sb.escape()
        val stringRange = document.createRangeMarker(fullStart, fullEnd)
        stringRange.isGreedyToRight = true
        document.deleteString(stringRange.startOffset, stringRange.endOffset)
        val documentText = document.charsSequence
        var addSpace = true
        val scanStart = stringRange.endOffset
        var scanEnd = scanStart
        val docLength = documentText.length
        while (scanEnd < docLength) {
            when(documentText[scanEnd]) {
                ' ', '\t', '\r', '\n' -> scanEnd++
                ',', ';' -> {
                    addSpace = false
                    break
                }
                else -> break
            }
        }
        if (scanEnd > scanStart) {
            if (addSpace) {
                document.replaceString(scanStart, scanEnd, " ")
            } else {
                document.deleteString(scanStart, scanEnd)
            }
        }

        document.insertString(stringRange.endOffset, "\"")
        document.insertString(stringRange.endOffset, prefixStr)
        caretOffset = stringRange.endOffset
        document.insertString(stringRange.endOffset, suffixStr)
        document.insertString(stringRange.endOffset, "\"")
        stringRange.dispose()
        editor.caretModel.moveToOffset(caretOffset)
    }
}
