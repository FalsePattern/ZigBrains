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