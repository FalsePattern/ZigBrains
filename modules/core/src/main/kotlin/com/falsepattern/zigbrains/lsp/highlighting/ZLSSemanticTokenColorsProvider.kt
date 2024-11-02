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

package com.falsepattern.zigbrains.lsp.highlighting

import com.falsepattern.zigbrains.lsp.highlighting.ZLSSemanticTokenModifiers.Declaration
import com.falsepattern.zigbrains.lsp.highlighting.ZLSSemanticTokenModifiers.Definition
import com.falsepattern.zigbrains.lsp.highlighting.ZLSSemanticTokenModifiers.Deprecated
import com.falsepattern.zigbrains.lsp.highlighting.ZLSSemanticTokenModifiers.Generic
import com.falsepattern.zigbrains.lsp.highlighting.ZLSSemanticTokenTypes.Builtin
import com.falsepattern.zigbrains.lsp.highlighting.ZLSSemanticTokenTypes.Comment
import com.falsepattern.zigbrains.lsp.highlighting.ZLSSemanticTokenTypes.Enum
import com.falsepattern.zigbrains.lsp.highlighting.ZLSSemanticTokenTypes.EnumMember
import com.falsepattern.zigbrains.lsp.highlighting.ZLSSemanticTokenTypes.ErrorTag
import com.falsepattern.zigbrains.lsp.highlighting.ZLSSemanticTokenTypes.Function
import com.falsepattern.zigbrains.lsp.highlighting.ZLSSemanticTokenTypes.Keyword
import com.falsepattern.zigbrains.lsp.highlighting.ZLSSemanticTokenTypes.KeywordLiteral
import com.falsepattern.zigbrains.lsp.highlighting.ZLSSemanticTokenTypes.Label
import com.falsepattern.zigbrains.lsp.highlighting.ZLSSemanticTokenTypes.Method
import com.falsepattern.zigbrains.lsp.highlighting.ZLSSemanticTokenTypes.Namespace
import com.falsepattern.zigbrains.lsp.highlighting.ZLSSemanticTokenTypes.Number
import com.falsepattern.zigbrains.lsp.highlighting.ZLSSemanticTokenTypes.Operator
import com.falsepattern.zigbrains.lsp.highlighting.ZLSSemanticTokenTypes.Parameter
import com.falsepattern.zigbrains.lsp.highlighting.ZLSSemanticTokenTypes.Property
import com.falsepattern.zigbrains.lsp.highlighting.ZLSSemanticTokenTypes.String
import com.falsepattern.zigbrains.lsp.highlighting.ZLSSemanticTokenTypes.Struct
import com.falsepattern.zigbrains.lsp.highlighting.ZLSSemanticTokenTypes.Type
import com.falsepattern.zigbrains.lsp.highlighting.ZLSSemanticTokenTypes.TypeParameter
import com.falsepattern.zigbrains.lsp.highlighting.ZLSSemanticTokenTypes.Variable
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.BUILTIN
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.ENUM_DECL
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.ENUM_MEMBER_DECL
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.ENUM_MEMBER_REF
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.ENUM_REF
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.ERROR_TAG_DECL
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.ERROR_TAG_REF
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.FUNCTION_DECL
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.FUNCTION_DECL_GEN
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.FUNCTION_REF
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.FUNCTION_REF_GEN
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.KEYWORD
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.LABEL_DECL
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.LABEL_REF
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.METHOD_DECL
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.METHOD_DECL_GEN
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.METHOD_REF
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.METHOD_REF_GEN
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.NAMESPACE_DECL
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.NAMESPACE_REF
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.NUMBER
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.OPERATOR
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.PARAMETER
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.PROPERTY_DECL
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.PROPERTY_REF
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.STRUCT_DECL
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.STRUCT_REF
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.TYPE_DECL
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.TYPE_DECL_GEN
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.TYPE_PARAM
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.TYPE_PARAM_DECL
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.TYPE_REF
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.TYPE_REF_GEN
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.VARIABLE_DECL
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.VARIABLE_DECL_DEPR
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.VARIABLE_REF
import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.Companion.VARIABLE_REF_DEPR
import com.intellij.psi.PsiFile
import com.redhat.devtools.lsp4ij.features.semanticTokens.DefaultSemanticTokensColorsProvider


class ZLSSemanticTokenColorsProvider : DefaultSemanticTokensColorsProvider() {
    override fun getTextAttributesKey(type: String, mod: List<String>, file: PsiFile) = with(mod) {
        when (type) {
            Builtin -> BUILTIN
            Enum -> if (isDecl) ENUM_DECL else ENUM_REF
            EnumMember -> if (isDecl) ENUM_MEMBER_DECL else ENUM_MEMBER_REF
            ErrorTag -> if (isDecl) ERROR_TAG_DECL else ERROR_TAG_REF
            Property -> if (isDecl) PROPERTY_DECL else PROPERTY_REF
            Function -> if (isDecl)
                if (has(Generic)) FUNCTION_DECL_GEN else FUNCTION_DECL
            else
                if (has(Generic)) FUNCTION_REF_GEN else FUNCTION_REF

            Keyword, KeywordLiteral -> KEYWORD
            Label -> if (isDecl) LABEL_DECL else LABEL_REF
            Method -> if (isDecl)
                if (has(Generic)) METHOD_DECL_GEN else METHOD_DECL
            else
                if (has(Generic)) METHOD_REF_GEN else METHOD_REF

            Namespace -> if (isDecl) NAMESPACE_DECL else NAMESPACE_REF
            Number -> NUMBER
            Operator -> OPERATOR
            Parameter -> PARAMETER
            Struct -> if (isDecl) STRUCT_DECL else STRUCT_REF
            Type -> if (isDecl)
                if (has(Generic)) TYPE_DECL_GEN else TYPE_DECL
            else
                if (has(Generic)) TYPE_REF_GEN else TYPE_REF

            TypeParameter -> if (isDecl) TYPE_PARAM_DECL else TYPE_PARAM
            Variable -> if (isDecl)
                if (has(Deprecated)) VARIABLE_DECL_DEPR else VARIABLE_DECL
            else
                if (has(Deprecated)) VARIABLE_REF_DEPR else VARIABLE_REF

            Comment, String -> null
            else -> super.getTextAttributesKey(type, mod, file)
        }
    }

}


private fun List<String>.has(key: String) = contains(key)

private val List<String>.isDecl get() = has(Declaration) || has(Definition)
