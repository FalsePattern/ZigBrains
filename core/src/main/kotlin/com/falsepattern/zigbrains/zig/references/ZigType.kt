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

package com.falsepattern.zigbrains.zig.references

import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter
import com.intellij.openapi.editor.colors.TextAttributesKey

enum class ZigType(val declaration: TextAttributesKey, val reference: TextAttributesKey) {
    Namespace(ZigSyntaxHighlighter.NAMESPACE_DECL, ZigSyntaxHighlighter.NAMESPACE_REF),
    ErrorType(ZigSyntaxHighlighter.TYPE_DECL, ZigSyntaxHighlighter.TYPE_REF),
    ErrorTag(ZigSyntaxHighlighter.ERROR_TAG_DECL, ZigSyntaxHighlighter.ERROR_TAG_REF),
    EnumType(ZigSyntaxHighlighter.ENUM_DECL, ZigSyntaxHighlighter.ENUM_REF),
    EnumMember(ZigSyntaxHighlighter.ENUM_MEMBER_DECL, ZigSyntaxHighlighter.ENUM_MEMBER_REF),
    Struct(ZigSyntaxHighlighter.STRUCT_DECL, ZigSyntaxHighlighter.STRUCT_REF),
    Union(ZigSyntaxHighlighter.STRUCT_DECL, ZigSyntaxHighlighter.STRUCT_REF),
    Generic(ZigSyntaxHighlighter.VARIABLE_DECL, ZigSyntaxHighlighter.VARIABLE_REF),
}