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

import org.eclipse.lsp4j.SemanticTokenTypes
import org.jetbrains.annotations.NonNls

@NonNls
object ZLSSemanticTokenTypes {
    const val Namespace: String = SemanticTokenTypes.Namespace
    const val Type: String = SemanticTokenTypes.Type
    const val Class: String = SemanticTokenTypes.Class
    const val Enum: String = SemanticTokenTypes.Enum
    const val Interface: String = SemanticTokenTypes.Interface
    const val Struct: String = SemanticTokenTypes.Struct
    const val TypeParameter: String = SemanticTokenTypes.TypeParameter
    const val Parameter: String = SemanticTokenTypes.Parameter
    const val Variable: String = SemanticTokenTypes.Variable
    const val Property: String = SemanticTokenTypes.Property
    const val EnumMember: String = SemanticTokenTypes.EnumMember
    const val Event: String = SemanticTokenTypes.Event
    const val Function: String = SemanticTokenTypes.Function
    const val Method: String = SemanticTokenTypes.Method
    const val Macro: String = SemanticTokenTypes.Macro
    const val Keyword: String = SemanticTokenTypes.Keyword
    const val Modifier: String = SemanticTokenTypes.Modifier
    const val Comment: String = SemanticTokenTypes.Comment
    const val String: String = SemanticTokenTypes.String
    const val Number: String = SemanticTokenTypes.Number
    const val Regexp: String = SemanticTokenTypes.Regexp
    const val Operator: String = SemanticTokenTypes.Operator
    const val Decorator: String = SemanticTokenTypes.Decorator

    /** non standard token type  */
    const val ErrorTag: String = "errorTag"

    /** non standard token type  */
    const val Builtin: String = "builtin"

    /** non standard token type  */
    const val Label: String = "label"

    /** non standard token type  */
    const val KeywordLiteral: String = "keywordLiteral"
}