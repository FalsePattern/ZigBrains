package com.falsepattern.zigbrains.zig.intentions

import com.falsepattern.zigbrains.zig.psi.ZigStringLiteral
import com.falsepattern.zigbrains.zig.psi.splitString
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType

class MakeStringMultiline: PsiElementBaseIntentionAction() {
    init {
        text = familyName
    }
    override fun getFamilyName() = "Convert to multiline"

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement) =
        editor != null && element.parentOfType<ZigStringLiteral>()?.isMultiline?.not() ?: false

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        editor ?: return
        val str = element.parentOfType<ZigStringLiteral>() ?: return
        if (str.isMultiline)
            return
        splitString(editor, str, editor.caretModel.offset, false)
    }
}