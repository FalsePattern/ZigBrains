package com.falsepattern.zigbrains.zon.comments

import com.falsepattern.zigbrains.zon.psi.ZonTypes
import com.intellij.codeInsight.generation.IndentedCommenter
import com.intellij.lang.CodeDocumentationAwareCommenter
import com.intellij.psi.PsiComment
import com.intellij.psi.tree.IElementType

class ZonCommenter: CodeDocumentationAwareCommenter, IndentedCommenter {
    override fun getLineCommentPrefix() = "// "

    override fun getBlockCommentPrefix() = null

    override fun getBlockCommentSuffix() = null

    override fun getCommentedBlockCommentPrefix() = null

    override fun getCommentedBlockCommentSuffix() = null

    override fun forceIndentedLineComment() = true

    override fun getLineCommentTokenType(): IElementType = ZonTypes.COMMENT

    override fun getBlockCommentTokenType() = null

    override fun getDocumentationCommentTokenType() = null

    override fun getDocumentationCommentPrefix() = null

    override fun getDocumentationCommentLinePrefix() = null

    override fun getDocumentationCommentSuffix() = null

    override fun isDocumentationComment(element: PsiComment?) = false
}