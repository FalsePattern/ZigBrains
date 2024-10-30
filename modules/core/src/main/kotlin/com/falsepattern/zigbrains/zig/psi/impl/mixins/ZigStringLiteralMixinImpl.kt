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

package com.falsepattern.zigbrains.zig.psi.impl.mixins

import com.falsepattern.zigbrains.zig.psi.*
import com.falsepattern.zigbrains.zig.util.decodeReplacements
import com.falsepattern.zigbrains.zig.util.unescape
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.impl.source.tree.LeafElement

abstract class ZigStringLiteralMixinImpl(node: ASTNode): ASTWrapperPsiElement(node), ZigStringLiteral {
    override fun isValidHost() = true

    override val isMultiline: Boolean
        get() = stringLiteralMulti != null

    override val contentRanges: List<TextRange>
        get() = if (!isMultiline) {
            listOf(TextRange(1, textLength - 1))
        } else {
            text.getMultilineContent("\\\\")
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
                for (i in contentRanges.indices) {
                    val range = rangeInsideHost.intersection(contentRanges[i]) ?: continue
                    last = range

                    val curString = range.substring(text)

                    val replacementsForThisLine = curString.decodeReplacements(myHost.isMultiline)
                    var encodedOffsetInCurrentLine = 0
                    for (replacement in replacementsForThisLine) {
                        val deltaLength = replacement.first.startOffset - encodedOffsetInCurrentLine
                        val currentOffsetBeforeReplacement = currentOffsetInDecoded + deltaLength
                        if (currentOffsetBeforeReplacement > offsetInDecoded) {
                            return range.startOffset + encodedOffsetInCurrentLine + (offsetInDecoded - currentOffsetInDecoded)
                        }
                        if (currentOffsetBeforeReplacement == offsetInDecoded && replacement.second.isNotEmpty()) {
                            return range.startOffset + encodedOffsetInCurrentLine + (offsetInDecoded - currentOffsetInDecoded)
                        }
                        currentOffsetInDecoded += deltaLength + replacement.second.length
                        encodedOffsetInCurrentLine += deltaLength + replacement.first.length
                    }

                    val deltaLength = curString.length - encodedOffsetInCurrentLine
                    if (currentOffsetInDecoded + deltaLength > offsetInDecoded) {
                        return range.startOffset + encodedOffsetInCurrentLine + (offsetInDecoded - currentOffsetInDecoded)
                    }
                    currentOffsetInDecoded += deltaLength
                }

                return last?.endOffset ?: -1
            }

            override fun isOneLine() = true

        }
    }
}