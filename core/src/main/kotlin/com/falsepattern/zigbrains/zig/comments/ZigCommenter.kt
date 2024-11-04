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

package com.falsepattern.zigbrains.zig.comments

import com.falsepattern.zigbrains.zig.psi.ZigTypes
import com.intellij.codeInsight.generation.IndentedCommenter
import com.intellij.lang.CodeDocumentationAwareCommenter
import com.intellij.psi.PsiComment

class ZigCommenter: CodeDocumentationAwareCommenter, IndentedCommenter {
    override fun getLineCommentPrefix() = COMMENT

    override fun getBlockCommentPrefix() = null

    override fun getBlockCommentSuffix() = null

    override fun getCommentedBlockCommentPrefix() = null

    override fun getCommentedBlockCommentSuffix() = null

    override fun forceIndentedLineComment() = true

    override fun getLineCommentTokenType() = ZigTypes.LINE_COMMENT!!

    override fun getBlockCommentTokenType() = null

    override fun getDocumentationCommentTokenType() = ZigTypes.DOC_COMMENT!!

    override fun getDocumentationCommentPrefix() = null

    override fun getDocumentationCommentLinePrefix() = DOC_COMMENT

    override fun getDocumentationCommentSuffix() = null

    override fun isDocumentationComment(element: PsiComment?): Boolean {
        val type = element?.tokenType ?: return false
        return type == ZigTypes.DOC_COMMENT || type == ZigTypes.CONTAINER_DOC_COMMENT
    }
}

const val COMMENT = "// "
const val DOC_COMMENT = "/// "