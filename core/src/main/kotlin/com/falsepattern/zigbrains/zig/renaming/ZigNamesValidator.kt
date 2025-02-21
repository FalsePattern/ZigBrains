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

package com.falsepattern.zigbrains.zig.renaming

import com.falsepattern.zigbrains.zig.lexer.ZigLexerAdapter
import com.falsepattern.zigbrains.zig.psi.ZigTypes
import com.intellij.lang.refactoring.NamesValidator
import com.intellij.openapi.project.Project
import com.intellij.psi.tree.IElementType

class ZigNamesValidator: NamesValidator {
    override fun isKeyword(name: String, project: Project?) = matches(name) { token ->
        keywords.contains(token)
    }

    override fun isIdentifier(name: String, project: Project?) = matches(name) { token ->
        token == ZigTypes.IDENTIFIER
    }

    private inline fun matches(name: String, matcher: (IElementType) -> Boolean): Boolean {
        val lexer = ZigLexerAdapter()
        lexer.start(name)
        val token = lexer.tokenType
        return token != null && lexer.tokenEnd == lexer.bufferEnd && matcher(token)
    }

    val keywords = ZigTypes::class.java.declaredFields.mapNotNullTo(HashSet<IElementType>()) { if (it.name.startsWith("KEYWORD_")) it.get(null) as IElementType else null }
}