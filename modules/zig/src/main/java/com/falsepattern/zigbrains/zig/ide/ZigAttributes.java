/*
 * Copyright 2023-2024 FalsePattern
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

package com.falsepattern.zigbrains.zig.ide;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import org.jetbrains.annotations.Unmodifiable;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.BUILTIN;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.COMMENT;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.COMMENT_DOC;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.ENUM_DECL;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.ENUM_MEMBER;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.ENUM_REF;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.ERROR_TAG;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.FUNCTION_REF;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.KEYWORD;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.LABEL_DECL;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.LABEL_REF;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.NAMESPACE_DECL;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.NAMESPACE_REF;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.NUMBER;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.OPERATOR;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.PARAMETER;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.PROPERTY;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.STRING;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.STRUCT_DECL;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.STRUCT_REF;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.TYPE_DECL;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.TYPE_DECL_GEN;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.TYPE_REF;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.TYPE_REF_GEN;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.VARIABLE_DECL;
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.VARIABLE_REF;


public enum ZigAttributes {
    Builtin(BUILTIN),
    Comment_Doc(COMMENT_DOC, "documentation"),
    Comment(COMMENT),
    Enum_Decl(ENUM_DECL, "declaration"),
    Enum(ENUM_REF),
    EnumMember(ENUM_MEMBER),
    ErrorTag(ERROR_TAG),
    Property(PROPERTY),
    Function_Decl_Gen(FUNCTION_REF, "declaration", "generic"),
    Function_Decl(FUNCTION_REF, "declaration"),
    Function_Gen(FUNCTION_REF, "generic"),
    Function(FUNCTION_REF),
    Keyword(KEYWORD),
    KeywordLiteral(KEYWORD),
    Label_Decl(LABEL_DECL, "declaration"),
    Label(LABEL_REF),
    Namespace_Decl(NAMESPACE_DECL, "declaration"),
    Namespace(NAMESPACE_REF),
    Number(NUMBER),
    Operator(OPERATOR),
    Parameter_Decl(PARAMETER, "declaration"),
    Parameter(PARAMETER),
    String(STRING),
    Struct_Decl(STRUCT_DECL, "declaration"),
    Struct(STRUCT_REF),
    Type_Decl_Gen(TYPE_DECL_GEN, "declaration", "generic"),
    Type_Decl(TYPE_DECL, "declaration"),
    Type_Gen(TYPE_REF_GEN, "generic"),
    Type(TYPE_REF),
    Variable_Decl(VARIABLE_DECL, "declaration"),
    Variable(VARIABLE_REF),
    ;
    public final TextAttributesKey KEY;
    public final String type;
    public final @Unmodifiable Set<String> modifiers;

    ZigAttributes(TextAttributesKey defaultKey, String... modifiers) {
        var name = name();
        int underscoreIndex = name.indexOf('_');
        type = Character.toLowerCase(name.charAt(0)) + (underscoreIndex > 0 ? name.substring(1, underscoreIndex) : name.substring(1));
        KEY = defaultKey;
        this.modifiers = new HashSet<>(List.of(modifiers));
    }

    public static Optional<TextAttributesKey> getKey(String type, Set<String> modifiers) {
        if (type == null) {
            return Optional.empty();
        }
        for (var known : values()) {
            if (known.type.equals(type) && ((modifiers != null && modifiers.containsAll(known.modifiers)) ||
                                            (modifiers == null && known.modifiers.size() == 0))) {
                return Optional.of(known.KEY);
            }
        }
        return Optional.empty();
    }
}
