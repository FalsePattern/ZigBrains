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

package com.falsepattern.zigbrains.zon.parser

import com.falsepattern.zigbrains.zon.ZonLanguage
import com.falsepattern.zigbrains.zon.lexer.ZonLexerAdapter
import com.falsepattern.zigbrains.zon.psi.ZonFile
import com.falsepattern.zigbrains.zon.psi.ZonTypes
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.tree.IFileElementType

class ZonParserDefinition: ParserDefinition {
    override fun createLexer(project: Project?) = ZonLexerAdapter()

    override fun createParser(project: Project?) = ZonParser()

    override fun getFileNodeType() = FILE

    override fun getCommentTokens() = ZonTokenSets.COMMENTS

    override fun getStringLiteralElements() = ZonTokenSets.STRINGS

    override fun createElement(node: ASTNode?) = ZonTypes.Factory.createElement(node)!!

    override fun createFile(viewProvider: FileViewProvider) = ZonFile(viewProvider)
}

val FILE = IFileElementType(ZonLanguage)