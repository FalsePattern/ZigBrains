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
        caretOffsetRef: Ref<Int>,
        caretAdvance: Ref<Int>,
        dataContext: DataContext,
        originalHandler: EditorActionHandler?
    ): Result {
        if (file !is ZigFile)
            return Result.Continue

        val caretOffset = caretOffsetRef.get()!!
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