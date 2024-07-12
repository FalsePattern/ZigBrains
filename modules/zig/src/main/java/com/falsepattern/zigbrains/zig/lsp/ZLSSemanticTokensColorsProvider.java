package com.falsepattern.zigbrains.zig.lsp;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.lsp4ij.features.semanticTokens.DefaultSemanticTokensColorsProvider;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.BUILTIN;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.COMMENT;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.COMMENT_DOC;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.ENUM_DECL;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.ENUM_MEMBER_DECL;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.ENUM_MEMBER_REF;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.ENUM_REF;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.ERROR_TAG_DECL;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.ERROR_TAG_REF;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.FUNCTION_DECL;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.FUNCTION_DECL_GEN;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.FUNCTION_REF;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.FUNCTION_REF_GEN;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.KEYWORD;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.LABEL_DECL;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.LABEL_REF;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.METHOD_DECL;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.METHOD_DECL_GEN;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.METHOD_REF;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.METHOD_REF_GEN;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.NAMESPACE_DECL;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.NAMESPACE_REF;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.NUMBER;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.OPERATOR;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.PARAMETER;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.PROPERTY_DECL;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.PROPERTY_REF;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.STRING;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.STRUCT_DECL;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.STRUCT_REF;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.TYPE_DECL;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.TYPE_DECL_GEN;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.TYPE_PARAM;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.TYPE_PARAM_DECL;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.TYPE_REF;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.TYPE_REF_GEN;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.VARIABLE_DECL;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.VARIABLE_DECL_DEPR;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.VARIABLE_REF;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.VARIABLE_REF_DEPR;
import static com.falsepattern.zigbrains.zig.lsp.ZLSSemanticTokenModifiers.Declaration;
import static com.falsepattern.zigbrains.zig.lsp.ZLSSemanticTokenModifiers.Definition;
import static com.falsepattern.zigbrains.zig.lsp.ZLSSemanticTokenModifiers.Deprecated;
import static com.falsepattern.zigbrains.zig.lsp.ZLSSemanticTokenModifiers.Documentation;
import static com.falsepattern.zigbrains.zig.lsp.ZLSSemanticTokenModifiers.Generic;
import static com.falsepattern.zigbrains.zig.lsp.ZLSSemanticTokenTypes.Builtin;
import static com.falsepattern.zigbrains.zig.lsp.ZLSSemanticTokenTypes.Comment;
import static com.falsepattern.zigbrains.zig.lsp.ZLSSemanticTokenTypes.Enum;
import static com.falsepattern.zigbrains.zig.lsp.ZLSSemanticTokenTypes.EnumMember;
import static com.falsepattern.zigbrains.zig.lsp.ZLSSemanticTokenTypes.ErrorTag;
import static com.falsepattern.zigbrains.zig.lsp.ZLSSemanticTokenTypes.Function;
import static com.falsepattern.zigbrains.zig.lsp.ZLSSemanticTokenTypes.Keyword;
import static com.falsepattern.zigbrains.zig.lsp.ZLSSemanticTokenTypes.KeywordLiteral;
import static com.falsepattern.zigbrains.zig.lsp.ZLSSemanticTokenTypes.Label;
import static com.falsepattern.zigbrains.zig.lsp.ZLSSemanticTokenTypes.Method;
import static com.falsepattern.zigbrains.zig.lsp.ZLSSemanticTokenTypes.Namespace;
import static com.falsepattern.zigbrains.zig.lsp.ZLSSemanticTokenTypes.Number;
import static com.falsepattern.zigbrains.zig.lsp.ZLSSemanticTokenTypes.Operator;
import static com.falsepattern.zigbrains.zig.lsp.ZLSSemanticTokenTypes.Parameter;
import static com.falsepattern.zigbrains.zig.lsp.ZLSSemanticTokenTypes.Property;
import static com.falsepattern.zigbrains.zig.lsp.ZLSSemanticTokenTypes.String;
import static com.falsepattern.zigbrains.zig.lsp.ZLSSemanticTokenTypes.Struct;
import static com.falsepattern.zigbrains.zig.lsp.ZLSSemanticTokenTypes.Type;
import static com.falsepattern.zigbrains.zig.lsp.ZLSSemanticTokenTypes.TypeParameter;
import static com.falsepattern.zigbrains.zig.lsp.ZLSSemanticTokenTypes.Variable;

public class ZLSSemanticTokensColorsProvider extends DefaultSemanticTokensColorsProvider {
    private record TokenHelper(List<String> tokenModifiers) {
        public boolean hasAny(String... keys) {
            if (tokenModifiers.isEmpty()) {
                return false;
            }
            for (val key : keys) {
                if (tokenModifiers.contains(key)) {
                    return true;
                }
            }
            return false;
        }

        public boolean has(String... keys) {
            if (tokenModifiers.isEmpty()) {
                return false;
            }
            for (val key : keys) {
                if (!tokenModifiers.contains(key)) {
                    return false;
                }
            }
            return true;
        }

        public boolean isDecl() {
            return hasAny(Declaration, Definition);
        }
    }

    @Override
    public @Nullable TextAttributesKey getTextAttributesKey(@NotNull String tokenType, @NotNull List<String> tokenModifiers, @NotNull PsiFile file) {
        val tok = new TokenHelper(tokenModifiers);
        val res = switch (tokenType) {
            case Builtin -> BUILTIN;
            case Comment -> tok.has(Documentation) ? COMMENT_DOC : COMMENT;
            case Enum -> tok.isDecl() ? ENUM_DECL : ENUM_REF;
            case EnumMember -> tok.isDecl() ? ENUM_MEMBER_DECL : ENUM_MEMBER_REF;
            case ErrorTag -> tok.isDecl() ? ERROR_TAG_DECL : ERROR_TAG_REF;
            case Property -> tok.isDecl() ? PROPERTY_DECL : PROPERTY_REF;
            case Function -> tok.isDecl() ? (tok.has(Generic) ? FUNCTION_DECL_GEN : FUNCTION_DECL)
                                          : (tok.has(Generic) ? FUNCTION_REF_GEN : FUNCTION_REF);
            case Keyword, KeywordLiteral -> KEYWORD;
            case Label -> tok.isDecl() ? LABEL_DECL : LABEL_REF;
            case Method -> tok.isDecl() ? (tok.has(Generic) ? METHOD_DECL_GEN : METHOD_DECL)
                                        : (tok.has(Generic) ? METHOD_REF_GEN : METHOD_REF);
            case Namespace -> tok.isDecl() ? NAMESPACE_DECL : NAMESPACE_REF;
            case Number -> NUMBER;
            case Operator -> OPERATOR;
            case Parameter -> PARAMETER;
            case String -> STRING;
            case Struct -> tok.isDecl() ? STRUCT_DECL : STRUCT_REF;
            case Type -> tok.isDecl() ? (tok.has(Generic) ? TYPE_DECL_GEN : TYPE_DECL)
                                      : (tok.has(Generic) ? TYPE_REF_GEN : TYPE_REF);
            case TypeParameter -> tok.isDecl() ? TYPE_PARAM_DECL : TYPE_PARAM;
            case Variable -> tok.isDecl() ? (tok.has(Deprecated) ? VARIABLE_DECL_DEPR : VARIABLE_DECL)
                                          : (tok.has(Deprecated) ? VARIABLE_REF_DEPR : VARIABLE_REF);
            default -> null;
        };
        return res != null ? res : super.getTextAttributesKey(tokenType, tokenModifiers, file);
    }
}
