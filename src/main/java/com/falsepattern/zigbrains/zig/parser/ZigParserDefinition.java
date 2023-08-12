/*
 * Copyright 2023 FalsePattern
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.falsepattern.zigbrains.zig.parser;

import com.falsepattern.zigbrains.zig.ide.ZigLanguage;
import com.falsepattern.zigbrains.zon.ZonLanguage;
import com.falsepattern.zigbrains.zon.lexer.ZonLexerAdapter;
import com.falsepattern.zigbrains.zon.parser.ZonParser;
import com.falsepattern.zigbrains.zon.parser.ZonTokenSets;
import com.falsepattern.zigbrains.zon.psi.ZonTypes;
import com.intellij.lang.ASTFactory;
import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.DummyLexer;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.fileTypes.PlainTextParserDefinition;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PlainTextTokenTypes;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ZigParserDefinition implements ParserDefinition {
    public static final IFileElementType FILE = new IFileElementType(ZigLanguage.INSTANCE) {
        public ASTNode parseContents(@NotNull ASTNode chameleon) {
            final CharSequence chars = chameleon.getChars();
            return ASTFactory.leaf(PlainTextTokenTypes.PLAIN_TEXT, chars);
        }
    };

    @Override
    public @NotNull Lexer createLexer(Project project) {
        return new DummyLexer(FILE);
    }

    @Override
    public @NotNull TokenSet getCommentTokens() {
        return TokenSet.EMPTY;
    }

    @Override
    public @NotNull TokenSet getStringLiteralElements() {
        return TokenSet.EMPTY;
    }

    @Override
    public @NotNull PsiParser createParser(Project project) {
        throw new UnsupportedOperationException("Not Supported");
    }

    @Override
    public @NotNull IFileElementType getFileNodeType() {
        return FILE;
    }

    @Override
    public @NotNull PsiFile createFile(@NotNull FileViewProvider viewProvider) {
        return new ZigFile(viewProvider);
    }

    @Override
    public @NotNull PsiElement createElement(ASTNode node) {
        return PsiUtilCore.NULL_PSI_ELEMENT;
    }
}
