package com.falsepattern.zigbrains.zig.parser

import com.falsepattern.zigbrains.zig.ZigLanguage
import com.falsepattern.zigbrains.zig.lexer.ZigLexerAdapter
import com.falsepattern.zigbrains.zig.psi.ZigFile
import com.falsepattern.zigbrains.zig.psi.ZigTypes
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.tree.IFileElementType

class ZigParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?) = ZigLexerAdapter()

    override fun createParser(project: Project?) = ZigParser()

    override fun getFileNodeType() = FILE

    override fun getCommentTokens() = ZigTokenSets.COMMENTS

    override fun getStringLiteralElements() = ZigTokenSets.STRINGS

    override fun createElement(node: ASTNode?) = ZigTypes.Factory.createElement(node)!!

    override fun createFile(viewProvider: FileViewProvider) = ZigFile(viewProvider)

}

val FILE = IFileElementType(ZigLanguage)