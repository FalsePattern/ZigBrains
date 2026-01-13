/*
 * ZigBrains
 * Copyright (C) 2023-2025 FalsePattern
 * All Rights Reserved
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.falsepattern.zigbrains.zig.intentions

import com.falsepattern.zigbrains.ZigBrainsBundle
import com.falsepattern.zigbrains.zig.psi.ZigParamDecl
import com.falsepattern.zigbrains.zig.psi.ZigTypes
import com.falsepattern.zigbrains.zig.psi.ZigVarDeclProto
import com.falsepattern.zigbrains.zig.psi.indentSize
import com.falsepattern.zigbrains.zig.psi.splitString
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore
import com.intellij.psi.util.elementType
import com.intellij.psi.util.endOffset
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.startOffset
import okhttp3.internal.delimiterOffset

class AssignToUnderscore: PsiElementBaseIntentionAction() {
    init {
        text = familyName
    }
    override fun getFamilyName() = ZigBrainsBundle.message("intention.family.name.assign-to-underscore")

	// only available in var/param decls
    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement) =
        editor != null && (element.parent is ZigVarDeclProto || element.parent is ZigParamDecl) && element.elementType == ZigTypes.IDENTIFIER

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        editor ?: return
		val doc = editor.document
		when ( element.parent ) {
			is ZigVarDeclProto -> {
				val end = element.endOffset
				val name = element.text
				val seq = doc.immutableCharSequence
				// get end of current line
				val nl = end + seq.subSequence( end, seq.length ).indexOfFirst { it == '\n' }
				// get start of current line
				val snl = doc.getLineStartOffset( doc.getLineNumber( end ) )  // start new line
				// refcopy the indentation
				val indent = seq.subSequence( snl, snl + seq.subSequence( snl, seq.length ).indexOfFirst { !it.isWhitespace() } )
				// insert the assignment
				doc.insertString( nl, "\n${indent}_ = $name;" )
			}
			is ZigParamDecl -> { // same as above, but we need to guess the indent and might need to expand `{}` into `{\n\i}`
				error("TODO: NOT IMPLEMENTED YET")
			}
		}
    }
}