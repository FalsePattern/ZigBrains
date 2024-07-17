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

public class ZLSSemanticTokensColorsProvider extends DefaultSemanticTokensColorsProvider {
    private record TokenHelper(List<String> tokenModifiers) {
        public boolean hasAny(String... keys) {
            if (tokenModifiers.isEmpty())
                return false;
            for (val key: keys) {
                if (tokenModifiers.contains(key))
                    return true;
            }
            return false;
        }

        public boolean has(String... keys) {
            if (tokenModifiers.isEmpty())
                return false;
            for (val key: keys) {
                if (!tokenModifiers.contains(key))
                    return false;
            }
            return true;
        }

        public boolean isDecl() {
            return hasAny("declaration", "definition");
        }
    }
    @Override
    public @Nullable TextAttributesKey getTextAttributesKey(@NotNull String tokenType,
                                                            @NotNull List<String> tokenModifiers,
                                                            @NotNull PsiFile file) {
        val tok = new TokenHelper(tokenModifiers);
        val res = switch (tokenType) {
            case "builtin" -> BUILTIN;
            case "comment" -> tok.has("documentation") ? COMMENT_DOC : COMMENT;
            case "enum" -> tok.isDecl() ? ENUM_DECL : ENUM_REF;
            case "enumMember" -> tok.isDecl() ? ENUM_MEMBER_DECL : ENUM_MEMBER_REF;
            case "errorTag" -> tok.isDecl() ? ERROR_TAG_DECL : ERROR_TAG_REF;
            case "property" -> tok.isDecl() ? PROPERTY_DECL : PROPERTY_REF;
            case "function" -> tok.isDecl() ? (tok.has("generic") ? FUNCTION_DECL_GEN : FUNCTION_DECL)
                                            : (tok.has("generic") ? FUNCTION_REF_GEN : FUNCTION_REF);
            case "keyword", "keywordLiteral" -> KEYWORD;
            case "label" -> tok.isDecl() ? LABEL_DECL : LABEL_REF;
            case "method" -> tok.isDecl() ? (tok.has("generic") ? METHOD_DECL_GEN : METHOD_DECL)
                                          : (tok.has("generic") ? METHOD_REF_GEN : METHOD_REF);
            case "namespace" -> tok.isDecl() ? NAMESPACE_DECL : NAMESPACE_REF;
            case "number" -> NUMBER;
            case "operator" -> OPERATOR;
            case "parameter" -> PARAMETER;
            case "string" -> STRING;
            case "struct" -> tok.isDecl() ? STRUCT_DECL : STRUCT_REF;
            case "type" -> tok.isDecl() ? (tok.has("generic") ? TYPE_DECL_GEN : TYPE_DECL)
                                        : (tok.has("generic") ? TYPE_REF_GEN : TYPE_REF);
            case "typeParameter" -> tok.isDecl() ? TYPE_PARAM_DECL : TYPE_PARAM;
            case "variable" -> tok.isDecl() ? (tok.has("deprecated") ? VARIABLE_DECL_DEPR : VARIABLE_DECL)
                                            : (tok.has("deprecated") ? VARIABLE_REF_DEPR : VARIABLE_REF);
            default -> null;
        };
        return res != null ? res : super.getTextAttributesKey(tokenType, tokenModifiers, file);
    }
}
