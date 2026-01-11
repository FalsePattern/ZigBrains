/*
 * This file is part of ZigBrains.
 *
 * Copyright (C) 2023-2026 FalsePattern
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

import com.falsepattern.zigbrains.zig.comments.cfg.ZigCommenterService
import com.falsepattern.zigbrains.zig.comments.cfg.ZigCommenterState
import com.falsepattern.zigbrains.zig.psi.ZigTypes
import com.intellij.codeInsight.generation.CommenterDataHolder
import com.intellij.codeInsight.generation.SelfManagingCommenter
import com.intellij.lang.CodeDocumentationAwareCommenter
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiFile
import com.intellij.util.text.CharArrayUtil
import kotlin.math.min

class ZigSelfManagingCommenter: SelfManagingCommenter<ZigCommenterDataHolder>, CodeDocumentationAwareCommenter {
	override fun getLineCommentPrefix() = COMMENT

	override fun getBlockCommentPrefix() = null

	override fun getBlockCommentSuffix() = null

	override fun getCommentedBlockCommentPrefix() = null

	override fun getCommentedBlockCommentSuffix() = null

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

	override fun createLineCommentingState(
		startLine: Int,
		endLine: Int,
		document: Document,
		file: PsiFile
	): ZigCommenterDataHolder? {
		var minIndent: Int? = null
		for (line in startLine..endLine) {
			val start = document.getLineStartOffset(line)
			if (isLineEmpty(document, line, start)) {
				continue
			}
			val offset = this.getOffset(document, line, start)
			val indent = offset - start
			minIndent = minIndent?.let { minOf(minIndent, indent) } ?: indent
		}
		val indent = minIndent ?: 0
		return ZigCommenterDataHolder(indent, ZigCommenterService.getInstance(file.project).commenterState)
	}

	override fun createBlockCommentingState(
		selectionStart: Int,
		selectionEnd: Int,
		document: Document,
		file: PsiFile
	): ZigCommenterDataHolder? =
		null

	//Only used for the hotkey commenting operations, so it shouldn't create doc comments.
	override fun commentLine(
		line: Int,
		offset: Int,
		document: Document,
		data: ZigCommenterDataHolder
	) {
		when(data.state) {
			ZigCommenterState.Standard -> {
				val lineEndOffset = document.getLineEndOffset(line)
				var commentOffset = offset + data.indent
				val b = StringBuilder()
				if (commentOffset >= lineEndOffset && isLineEmpty(document, line, offset, lineEndOffset)) {
					for (i in lineEndOffset..<commentOffset) {
						b.append(' ')
					}
					commentOffset = lineEndOffset
				}
				b.append(COMMENT).append(' ')
				document.insertString(commentOffset, b.toString())
			}
			ZigCommenterState.Alternative -> {
				val prefix = if ( line == 0 )
					TOP_LEVEL_COMMENT
				else
					this.detectLineCommentType(document, line - 1) ?: COMMENT

				val offset = this.getOffset(document, line, offset)
				document.insertString(offset, "$prefix ")
			}
		}
	}

	override fun uncommentLine(
		line: Int,
		offset: Int,
		document: Document,
		data: ZigCommenterDataHolder
	) {
		// how much text do we need to remove?
		val prefix = this.detectLineCommentType(document, line) ?: return

		when(data.state) {
			ZigCommenterState.Standard -> {
				val lineEndOffset = document.getLineEndOffset(line)

				val prefixStartOffset = this.getOffset(document, line, offset)
				val prefixEndOffset = prefixStartOffset + prefix.length

				val offsetAfterPrefix = this.getOffset(document, line, prefixEndOffset)

				if (offsetAfterPrefix == lineEndOffset) {
					//empty line, intellij/zig fmt strips whitespace in empty lines, follow the same convention
					document.deleteString(document.getLineStartOffset(line), lineEndOffset)
					return
				}

				//strip the first space after the comment (zig convention)
				val cs = document.immutableCharSequence
				val endOffset = if (cs.length > prefixEndOffset && cs[prefixEndOffset].isWhitespace()) prefixEndOffset + 1 else prefixEndOffset

				document.deleteString(prefixStartOffset, endOffset)
			}
			ZigCommenterState.Alternative -> {
				val offset = this.getOffset(document, line, offset)
				val endOffset = this.getOffset(document, line, offset + prefix.length)

				document.deleteString(offset, endOffset)
				this.deleteSpacesIfLineIsBlank(document, document.charsSequence, line)
			}
		}
	}

	override fun isLineCommented(
		line: Int,
		offset: Int,
		document: Document,
		data: ZigCommenterDataHolder
	): Boolean =
		this.detectLineCommentType(document, line) != null

	override fun getCommentPrefix(
		line: Int,
		document: Document,
		data: ZigCommenterDataHolder
	): String? =
		if ( line != 0 ) this.detectLineCommentType(document, line - 1) else TOP_LEVEL_COMMENT

	override fun getBlockCommentRange(
		selectionStart: Int,
		selectionEnd: Int,
		document: Document,
		data: ZigCommenterDataHolder
	): TextRange? =
		null

	override fun getBlockCommentPrefix(
		selectionStart: Int,
		document: Document,
		data: ZigCommenterDataHolder
	): String? =
		null

	override fun getBlockCommentSuffix(
		selectionEnd: Int,
		document: Document,
		data: ZigCommenterDataHolder
	): String? =
		null

	override fun uncommentBlockComment(
		startOffset: Int,
		endOffset: Int,
		document: Document?,
		data: ZigCommenterDataHolder?
	) =
		Unit

	override fun insertBlockComment(
		startOffset: Int,
		endOffset: Int,
		document: Document?,
		data: ZigCommenterDataHolder?
	): TextRange? =
		null

	private fun getOffset(document: Document, line: Int, offset: Int = document.getLineStartOffset(line)): Int {
		val sequence = document.immutableCharSequence
		val len = sequence.length
		var offset = offset

		// skip whitespace
		while ( offset < len && sequence[offset].isWhitespace() && sequence[offset] != '\r' && sequence[offset] != '\n' ) {
			offset += 1
		}

		return offset
	}

	private fun isLineEmpty(document: Document, line: Int, startOffset: Int = document.getLineStartOffset(line), endOffset: Int = document.getLineEndOffset(line)): Boolean {
		val sequence = document.immutableCharSequence
		for (offset in startOffset..endOffset) {
			if (!sequence[offset].isWhitespace())
				return false
		}
		return true
	}

	private fun detectLineCommentType(document: Document, line: Int): String? {
		val sequence = document.immutableCharSequence
		val offset = this.getOffset(document, line)


		// copy-less way of checking the document
		return when {
			CharArrayUtil.regionMatches(sequence, offset, TOP_LEVEL_COMMENT ) -> TOP_LEVEL_COMMENT
			CharArrayUtil.regionMatches(sequence, offset, DOC_COMMENT ) -> DOC_COMMENT
			CharArrayUtil.regionMatches(sequence, offset, COMMENT ) -> COMMENT
			else -> null
		}
	}

	/**
	 * Delete whitespace on a line if that's all that left after uncommenting
	 *
	 * copied from [com.intellij.codeInsight.generation.CommentByLineCommentHandler.doUncommentLine]
	 * since it is not applicable to us because we inherited SelfManagingCommenter
	 */
	private fun deleteSpacesIfLineIsBlank(document: Document, documentText: CharSequence, line: Int) {
		val lineStartOffset = document.getLineStartOffset(line)
		val lineEndOffset = document.getLineEndOffset(line)
		if (CharArrayUtil.isEmptyOrSpaces(documentText, lineStartOffset, lineEndOffset)) {
			document.deleteString(lineStartOffset, lineEndOffset)
		}
	}

	companion object {
		const val COMMENT = "//"
		const val DOC_COMMENT = "///"
		const val TOP_LEVEL_COMMENT = "//!"
	}
}
