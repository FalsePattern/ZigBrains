package com.falsepattern.zigbrains.zig.psi.impl.mixins

import com.falsepattern.zigbrains.zig.psi.*
import com.falsepattern.zigbrains.zig.util.decodeReplacements
import com.falsepattern.zigbrains.zig.util.unescape
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.impl.source.tree.LeafElement
import java.lang.StringBuilder

abstract class ZigStringLiteralMixinImpl(node: ASTNode): ASTWrapperPsiElement(node), ZigStringLiteral {
    override fun isValidHost() = true

    override val isMultiline: Boolean
        get() = stringLiteralMulti != null

    override val contentRanges: List<TextRange>
        get() = if (!isMultiline) {
            listOf(TextRange(1, textLength - 1))
        } else {
            text.getMultiLineContent("\\\\")
        }

    override fun updateText(text: String): ZigStringLiteral {
        (stringLiteralSingle ?: stringLiteralMulti)
            ?.let { it as LeafElement }
            ?.replaceWithText(text)
        return this
    }

    override fun createLiteralTextEscaper(): LiteralTextEscaper<out ZigStringLiteral> {
        return object: LiteralTextEscaper<ZigStringLiteral>(this) {
            private var _text: String? = null
            private var _contentRanges: List<TextRange>? = null

            private val text: String
                get() = _text ?: myHost.text.also { _text = it }
            private val contentRanges: List<TextRange>
                get() = _contentRanges ?: myHost.contentRanges.also { _contentRanges = it }

            override fun decode(rangeInsideHost: TextRange, outChars: StringBuilder): Boolean {
                val text = myHost.text.also { _text = it }
                val isMultiline = myHost.isMultiline
                val contentRanges = myHost.contentRanges.also { _contentRanges = it }
                var decoded = false;
                for (range in contentRanges) {
                    val intersection = range.intersection(rangeInsideHost) ?: continue
                    decoded = true
                    val substring = intersection.subSequence(text)
                    outChars.append(substring.unescape(isMultiline))
                }
                return decoded
            }

            override fun getRelevantTextRange() = getTextRangeBounds(contentRanges)

            override fun getOffsetInHost(offsetInDecoded: Int, rangeInsideHost: TextRange): Int {
                var currentOffsetInDecoded = 0

                var last: TextRange? = null
                val isMultiline = myHost.isMultiline
                for (range in contentRanges) {
                    last = range

                    val (rangeStart, _) = range

                    val curString = range.subSequence(text)

                    val replacementsForThisLine = curString.decodeReplacements(isMultiline)
                    var encodedOffsetInCurrentLine = 0
                    for ((thisRange, replacement) in replacementsForThisLine) {
                        val (thisStart, _) = thisRange
                        val deltaLength = thisStart - encodedOffsetInCurrentLine
                        val currentOffsetBeforeReplacement = currentOffsetInDecoded + deltaLength
                        if (currentOffsetBeforeReplacement > offsetInDecoded) {
                            return thisStart + encodedOffsetInCurrentLine + (offsetInDecoded - currentOffsetInDecoded)
                        }
                        if (currentOffsetBeforeReplacement == offsetInDecoded && replacement.isNotEmpty()) {
                            return thisStart + encodedOffsetInCurrentLine + (offsetInDecoded - currentOffsetInDecoded)
                        }
                        currentOffsetInDecoded += deltaLength + replacement.length
                        encodedOffsetInCurrentLine += deltaLength + range.length
                    }

                    val deltaLength = curString.length - encodedOffsetInCurrentLine
                    if (currentOffsetInDecoded + deltaLength > offsetInDecoded) {
                        return rangeStart + encodedOffsetInCurrentLine + (offsetInDecoded - currentOffsetInDecoded)
                    }
                    currentOffsetInDecoded += deltaLength
                }

                return last?.endOffset ?: -1
            }

            override fun isOneLine() = true

        }
    }
}