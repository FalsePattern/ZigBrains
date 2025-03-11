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

import com.falsepattern.zigbrains.zig.psi.ZigStringLiteral
import com.falsepattern.zigbrains.zig.psi.ZigTypes
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.tree.IElementType

val ASSISTANTS = listOf(
    StringAssistant,
    CommentAssistant("//", ZigTypes.LINE_COMMENT),
    CommentAssistant("///", ZigTypes.DOC_COMMENT),
    CommentAssistant("//!", ZigTypes.CONTAINER_DOC_COMMENT)
)

interface ZigMultilineAssistant<T: PsiElement> {
    val prefix: String
    fun acceptPSI(element: PsiElement): T?
}

object StringAssistant: ZigMultilineAssistant<ZigStringLiteral> {
    override val prefix get() = "\\\\"
    override fun acceptPSI(element: PsiElement): ZigStringLiteral? {
        val candidate = when (element) {
            is LeafPsiElement -> {
                if (element.elementType == ZigTypes.STRING_LITERAL_MULTI) {
                    element.parent
                } else {
                    return null
                }
            }

            else -> element
        }

        return if (candidate is ZigStringLiteral && candidate.isMultiline)
            candidate
        else
            null
    }
}

class CommentAssistant(override val prefix: String, private val tokenType: IElementType): ZigMultilineAssistant<PsiComment> {
    override fun acceptPSI(element: PsiElement) =
        if (element is PsiComment && element.tokenType == tokenType) {
            element
        } else {
            null
        }
}