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

package com.falsepattern.zigbrains.zig.intentions

import com.falsepattern.zigbrains.ZigBrainsBundle
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
    override fun getFamilyName() = ZigBrainsBundle.message("intention.family.name.make-string-multiline")

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement) =
        editor != null && element.parentOfType<ZigStringLiteral>()?.isMultiline?.not() == true

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        editor ?: return
        val str = element.parentOfType<ZigStringLiteral>() ?: return
        if (str.isMultiline)
            return
        splitString(editor, str, editor.caretModel.offset, false)
    }
}