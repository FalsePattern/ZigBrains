package com.falsepattern.zigbrains.zig.comments

import com.falsepattern.zigbrains.zig.psi.ZigTypes
import com.intellij.codeInsight.generation.IndentedCommenter
import com.intellij.lang.CodeDocumentationAwareCommenter
import com.intellij.psi.PsiComment
import com.intellij.psi.tree.IElementType

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