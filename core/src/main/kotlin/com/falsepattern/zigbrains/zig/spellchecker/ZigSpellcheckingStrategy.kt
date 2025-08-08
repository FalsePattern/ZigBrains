/*
 * ZigBrains
 * Copyright (C) 2023-2025 FalsePattern
 * All Rights Reserved
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.falsepattern.zigbrains.zig.spellchecker

import com.falsepattern.zigbrains.zig.ZigLanguage
import com.falsepattern.zigbrains.zig.psi.ZigStringLiteral
import com.falsepattern.zigbrains.zon.ZonLanguage
import com.falsepattern.zigbrains.zon.psi.ZonStringLiteral
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.spellchecker.inspections.PlainTextSplitter
import com.intellij.spellchecker.tokenizer.*

class ZigSpellcheckingStrategy: SpellcheckingStrategy() {
	override fun isMyContext(element: PsiElement): Boolean {
		return element.language is ZigLanguage || element.language is ZonLanguage
	}

	override fun getTokenizer(element: PsiElement): Tokenizer<*> {
		return when (element) {
			is ZonStringLiteral -> ZonStringLiteralTokenizer
			is ZigStringLiteral -> ZigStringLiteralTokenizer
			is PsiNameIdentifierOwner -> IdentifierOwnerTokenizer
			else -> super.getTokenizer(element)
		}
	}
}

private object IdentifierOwnerTokenizer : PsiIdentifierOwnerTokenizer() {
	override fun tokenize(element: PsiNameIdentifierOwner, consumer: TokenConsumer) {
		val id = element.nameIdentifier
		if (id !is ZigStringLiteral && id !is ZonStringLiteral) {
			super.tokenize(element, consumer)
		}
	}
}

private object ZigStringLiteralTokenizer : EscapeSequenceTokenizer<ZigStringLiteral>() {
	override fun tokenize(element: ZigStringLiteral, consumer: TokenConsumer) {
		if (element.isMultiline) {
			consumer.consumeToken(element, PlainTextSplitter.getInstance())
		}
	}
}
private object ZonStringLiteralTokenizer : EscapeSequenceTokenizer<ZonStringLiteral>() {
	override fun tokenize(element: ZonStringLiteral, consumer: TokenConsumer) {
		if (element.isMultiline) {
			consumer.consumeToken(element, PlainTextSplitter.getInstance())
		}
	}
}
