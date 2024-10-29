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

class ZigStringElementManipulator: AbstractElementManipulator<ZigStringLiteral>() {
    override fun handleContentChange(
        element: ZigStringLiteral,
        range: TextRange,
        newContent: String?
    ): ZigStringLiteral {
        val originalContext = element.text!!
        val isMultiline = element.isMultiline
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
    var i = 0
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