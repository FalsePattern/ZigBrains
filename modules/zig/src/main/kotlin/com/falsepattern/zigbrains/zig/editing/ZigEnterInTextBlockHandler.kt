package com.falsepattern.zigbrains.zig.editing

import com.falsepattern.zigbrains.zig.psi.ZigFile
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate.Result
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class ZigEnterInTextBlockHandler : EnterHandlerDelegateAdapter() {
    override fun preprocessEnter(
        file: PsiFile,
        editor: Editor,
        caretOffset: Ref<Int>,
        caretAdvance: Ref<Int>,
        dataContext: DataContext,
        originalHandler: EditorActionHandler?
    ): Result {
        if (file !is ZigFile)
            return Result.Continue
        for (assistant in ASSISTANTS) {
            return assistant.preprocessEnter(file, editor) ?: continue
        }
        return Result.Continue
    }
}

private fun <T : PsiElement> ZigMultilineAssistant<T>.preprocessEnter(
    file: PsiFile,
    editor: Editor
): Result? {
    val offset = editor.caretModel.offset
    val textBlock = textBlockAt(file, offset) ?: return null

    val project = textBlock.project
    val document = editor.document

    val textBlockOffset = textBlock.textOffset
    val textBlockLine = document.getLineNumber(textBlockOffset)
    val textBlockLineStart = document.getLineStartOffset(textBlockLine)
    val indentPre = textBlockOffset - textBlockLineStart
    val lineNumber = document.getLineNumber(offset)
    val lineStartOffset = document.getLineStartOffset(lineNumber)
    val text = document.getText(TextRange(lineStartOffset, offset + 1))
    val parts = text.split(prefix, limit = 2).toMutableList()
    if (parts.size != 2)
        return Result.Continue

    val indentPost = parts[1].measureSpaces()
    val newLine = StringBuilder(1 + indentPre + prefix.length + indentPost)
        .append('\n')
        .repeat(' '.code, indentPre)
        .append(prefix)
        .repeat(' '.code, indentPost)
    document.insertString(offset, newLine)
    PsiDocumentManager.getInstance(project).commitDocument(document)
    editor.caretModel.moveToOffset(offset + newLine.length)
    return Result.Stop
}

private fun CharSequence.measureSpaces(): Int {
    this.forEachIndexed {index, c ->
        when(c) {
            ' ', '\t' -> {}
            else -> return index
        }
    }
    return length
}

private fun <T: PsiElement> ZigMultilineAssistant<T>.textBlockAt(
    file: PsiFile,
    offset: Int,
    ): T? =
    file.findElementAt(offset)?.let { acceptPSI(it) }