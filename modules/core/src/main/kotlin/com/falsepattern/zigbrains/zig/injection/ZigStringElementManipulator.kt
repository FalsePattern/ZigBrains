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

package com.falsepattern.zigbrains.zig.injection

import com.falsepattern.zigbrains.zig.ZigFileType
import com.falsepattern.zigbrains.zig.injection.InjectTriState.*
import com.falsepattern.zigbrains.zig.psi.ZigContainerMembers
import com.falsepattern.zigbrains.zig.psi.ZigPrimaryTypeExpr
import com.falsepattern.zigbrains.zig.psi.ZigStringLiteral
import com.falsepattern.zigbrains.zig.psi.getTextRangeBounds
import com.falsepattern.zigbrains.zig.psi.indentSize
import com.falsepattern.zigbrains.zig.util.escape
import com.falsepattern.zigbrains.zig.util.prefixWithTextBlockEscape
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator
import com.intellij.psi.PsiFileFactory
import org.jetbrains.annotations.NonNls

class ZigStringElementManipulator: AbstractElementManipulator<ZigStringLiteral>() {
    override fun handleContentChange(
        element: ZigStringLiteral,
        range: TextRange,
        newContent: String?
    ): ZigStringLiteral {
        val originalContext = element.text!!
        val isMultiline = element.isMultiline
        @NonNls
        val prefix = "const x = \n";
        val suffix = "\n;"
        val sbFactory: (Int) -> StringBuilder = {
            val sb = StringBuilder(prefix.length + suffix.length + it)
            sb.append(prefix)
            sb
        }
        val replacement = if (isMultiline) {
            replaceMultilineContent(element, newContent, range, originalContext, sbFactory)
        } else {
            replaceQuotedContent(element, range, newContent, originalContext, sbFactory)
        }
        replacement.append(suffix)
        val fileFactory = PsiFileFactory.getInstance(element.project)
        val dummy = fileFactory.createFileFromText(fileName, ZigFileType, replacement)
        val stringLiteral = dummy
            .firstChild
            .let {it as ZigContainerMembers}
            .containerDeclarationsList
            .first()
            .declList
            .first()
            .globalVarDecl!!
            .expr
            .let { it as ZigPrimaryTypeExpr }
            .stringLiteral!!
        return element.replace(stringLiteral) as ZigStringLiteral
    }


    override fun getRangeInElement(element: ZigStringLiteral) =
        getTextRangeBounds(element.contentRanges)
}

@NonNls
private val fileName = "dummy." + ZigFileType.defaultExtension

private fun ZigStringElementManipulator.replaceQuotedContent(
    element: ZigStringLiteral,
    range: TextRange,
    newContent: String?,
    originalContext: String,
    sbFactory: (Int) -> StringBuilder
): StringBuilder {
    val elementRange = getRangeInElement(element)
    val prefixStart = elementRange.startOffset
    val prefixEnd = range.startOffset
    val suffixStart = range.endOffset
    val suffixEnd = elementRange.endOffset
    val escaped = newContent?.escape()
    val result = sbFactory(2 + (prefixEnd - prefixStart) + (escaped?.length ?: 0) + (suffixEnd - suffixStart))
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
    element: ZigStringLiteral,
    newContent: String?,
    range: TextRange,
    originalContext: String,
    sbFactory: (Int) -> StringBuilder
): StringBuilder {
    val contentRanges = element.contentRanges
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
        indent = element.indentSize,
        marker = "\\\\",
        indentFirst = false,
        prefixFirst = true,
        newLineAfter = false,
        sbFactory
    )
}