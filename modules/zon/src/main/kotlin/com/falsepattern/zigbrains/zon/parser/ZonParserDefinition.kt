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