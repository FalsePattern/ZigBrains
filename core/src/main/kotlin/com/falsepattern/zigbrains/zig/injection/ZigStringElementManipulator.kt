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

package com.falsepattern.zigbrains.zig.injection

import com.falsepattern.zigbrains.startOffsetInAncestor
import com.falsepattern.zigbrains.zig.ZigFileType
import com.falsepattern.zigbrains.zig.injection.InjectTriState.*
import com.falsepattern.zigbrains.zig.psi.*
import com.falsepattern.zigbrains.zig.util.escape
import com.falsepattern.zigbrains.zig.util.prefixWithTextBlockEscape
import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.childLeafs
import com.intellij.util.asSafely
import org.jetbrains.annotations.NonNls

class ZigStringElementManipulator: AbstractElementManipulator<ZigStringLiteral>() {
    override fun handleContentChange(
        element: ZigStringLiteral,
        range: TextRange,
        newContent: String?
    ): ZigStringLiteral {
        val child = element.findElementAt(range.startOffset)?.asSafely<LeafPsiElement>()?.takeIf { it.elementType == ZigTypes.STRING_LITERAL_MULTI || it.elementType == ZigTypes.STRING_LITERAL_SINGLE } ?: throw IllegalArgumentException()
        val childElemOffset = child.startOffsetInAncestor(element)
        val originalContext = child.text
        val childElemLength = child.textLength
        val childInParentRange = TextRange(childElemOffset, childElemOffset + childElemLength)
        val contentRanges = element.contentRanges.mapNotNull { it.intersection(childInParentRange)?.shiftLeft(childElemOffset) }
        if (contentRanges.isEmpty()) {
            return element
        }
        val rangeInChild = range.shiftLeft(childElemOffset)
        if (rangeInChild.endOffset > childElemLength) {
            return element
        }

        val replacement = if (element.isMultiline) {
            val last = element.childLeafs().lastOrNull { it is LeafPsiElement && it.elementType == ZigTypes.STRING_LITERAL_MULTI }
            val sb = replaceMultilineContent(element.indentSize, contentRanges, newContent, rangeInChild, originalContext, child == last)
            sb.append('\n')
        } else {
            if (contentRanges.size != 1) {
                return element
            }
            replaceQuotedContent(contentRanges[0], rangeInChild, newContent, originalContext)
        }
        child.replaceWithText(replacement.toString())
        return element
    }


    override fun getRangeInElement(element: ZigStringLiteral) =
        getTextRangeBounds(element.contentRanges)
}

@NonNls
private val fileName = "dummy." + ZigFileType.defaultExtension

private fun ZigStringElementManipulator.replaceQuotedContent(
    contentRange: TextRange,
    range: TextRange,
    newContent: String?,
    originalContext: String
): StringBuilder {
    val prefixStart = contentRange.startOffset
    val prefixEnd = range.startOffset
    val suffixStart = range.endOffset
    val suffixEnd = contentRange.endOffset
    val escaped = newContent?.escape()
    val result = StringBuilder(2 + (prefixEnd - prefixStart) + (escaped?.length ?: 0) + (suffixEnd - suffixStart))
    result.append('"')
    result.append(originalContext.subSequence(prefixStart, prefixEnd))
    if (escaped != null) {
        result.append(escaped)
    }
    result.append(originalContext.subSequence(suffixStart, suffixEnd))
    result.append('"')
    return result
}

private enum class InjectTriState {
    NotYet,
    Incomplete,
    Complete
}

private fun replaceMultilineContent(
    indent: Int,
    contentRanges: List<TextRange>,
    newContent: String?,
    range: TextRange,
    originalContext: String,
    isLast: Boolean
): StringBuilder {
    val contentBuilder = StringBuilder(contentRanges.sumOf { it.length } + (newContent?.length ?: 0))
    var injectState = NotYet
    val contentIter = contentRanges.iterator()
    while (injectState === NotYet && contentIter.hasNext()) {
        val contentRange = contentIter.next()
        val intersection = contentRange.intersection(range)
        if (intersection == null) {
            contentBuilder.append(originalContext, contentRange.startOffset, contentRange.endOffset)
            continue
        }
        contentBuilder.append(originalContext, contentRange.startOffset, intersection.startOffset)
        contentBuilder.append(newContent)
        if (intersection.endOffset < contentRange.endOffset) {
            contentBuilder.append(originalContext, intersection.endOffset, contentRange.endOffset)
            injectState = Complete
        } else {
            injectState = Incomplete
        }
    }
    while (injectState === Incomplete && contentIter.hasNext()) {
        val contentRange = contentIter.next()
        val intersection = contentRange.intersection(range)
        if (intersection == null) {
            contentBuilder.append(originalContext, contentRange.startOffset, contentRange.endOffset)
        } else if (intersection.endOffset < contentRange.endOffset) {
            contentBuilder.append(originalContext, intersection.endOffset, contentRange.endOffset)
            injectState = Complete
        }
    }
    while (contentIter.hasNext()) {
        val contentRange = contentIter.next()
        contentBuilder.append(originalContext, contentRange.startOffset, contentRange.endOffset)
    }
    return contentBuilder.prefixWithTextBlockEscape(
        indent = indent,
        marker = "\\\\",
        indentFirst = false,
        prefixFirst = true,
        newLineAfter = false,
        stripLastNewline = !isLast
    )
}