package com.falsepattern.zigbrains.zig.editing

import com.falsepattern.zigbrains.zig.psi.ZigFile
import com.falsepattern.zigbrains.zig.psi.ZigStringLiteral
import com.falsepattern.zigbrains.zig.psi.ZigTypes
import com.falsepattern.zigbrains.zig.psi.splitString
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate.Result
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement

class ZigEnterInQuotedStringHandler: EnterHandlerDelegateAdapter() {
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

        val caretOffset = caretOffset.get()!!
        var psiAtOffset = file.findElementAt(caretOffset) ?: return Result.Continue
        if (psiAtOffset is LeafPsiElement) {
            if (psiAtOffset.elementType == ZigTypes.STRING_LITERAL_SINGLE) {
                psiAtOffset = psiAtOffset.parent ?: return Result.Continue
            }
        }
        if (psiAtOffset is ZigStringLiteral &&
            !psiAtOffset.isMultiline &&
            psiAtOffset.textOffset < caretOffset) {
            splitString(editor, psiAtOffset, caretOffset, true)
            return Result.Stop
        }
        return Result.Continue
    }
}