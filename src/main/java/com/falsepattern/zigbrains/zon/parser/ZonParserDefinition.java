package com.falsepattern.zigbrains.zon.parser;

import com.falsepattern.zigbrains.zon.ZonLanguage;
import com.falsepattern.zigbrains.zon.lexer.ZonLexerAdapter;
import com.falsepattern.zigbrains.zon.psi.ZonTypes;
import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

public class ZonParserDefinition implements ParserDefinition {
    public static final IFileElementType FILE = new IFileElementType(ZonLanguage.INSTANCE);
    @Override
    public @NotNull Lexer createLexer(Project project) {
        return new ZonLexerAdapter();
    }

    @Override
    public @NotNull TokenSet getCommentTokens() {
        return ZonTokenSets.COMMENTS;
    }

    @Override
    public @NotNull TokenSet getStringLiteralElements() {
        return ZonTokenSets.STRINGS;
    }

    @Override
    public @NotNull PsiParser createParser(Project project) {
        return new ZonParser();
    }

    @Override
    public @NotNull IFileElementType getFileNodeType() {
        return FILE;
    }

    @Override
    public @NotNull PsiFile createFile(@NotNull FileViewProvider viewProvider) {
        return new ZonFile(viewProvider);
    }

    @Override
    public @NotNull PsiElement createElement(ASTNode node) {
        return ZonTypes.Factory.createElement(node);
    }
}
