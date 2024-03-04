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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import kotlin.Pair;
import lombok.val;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
import static com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter.VARIABLE_REF;


public enum ZigAttributes {
    Builtin(BUILTIN),
    Comment_Doc(COMMENT_DOC, "documentation"),
    Comment(COMMENT),
    Enum_Decl(ENUM_DECL, "declaration"),
    Enum(ENUM_REF),
    EnumMember_Decl(ENUM_MEMBER_DECL, "declaration"),
    EnumMember(ENUM_MEMBER_REF),
    ErrorTag_Decl(ERROR_TAG_DECL, "declaration"),
    ErrorTag(ERROR_TAG_REF),
    Property_Decl(PROPERTY_DECL, "declaration"),
    Property(PROPERTY_REF),
    Function_Decl_Gen(FUNCTION_DECL_GEN, "declaration", "generic"),
    Function_Decl(FUNCTION_DECL, "declaration"),
    Function_Gen(FUNCTION_REF_GEN, "generic"),
    Function(FUNCTION_REF),
    Keyword(KEYWORD),
    KeywordLiteral(KEYWORD),
    Label_Decl(LABEL_DECL, "declaration"),
    Label(LABEL_REF),
    Method_Decl_Gen(METHOD_DECL_GEN, "declaration", "generic"),
    Method_Decl(METHOD_DECL, "declaration"),
    Method_Gen(METHOD_REF_GEN, "generic"),
    Method(METHOD_REF),
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
    TypeParameter_Decl(TYPE_PARAM_DECL, "declaration"),
    TypeParameter(TYPE_PARAM),
    Variable_Decl_Depr(VARIABLE_DECL, "declaration", "deprecated"),
    Variable_Decl(VARIABLE_DECL, "declaration"),
    Variable_Depr(VARIABLE_REF, "deprecated"),
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

    private static final Map<String, List<ZigAttributes>> types = new HashMap<>();
    static {
        for (val known: values()) {
            types.computeIfAbsent(known.type, (ignored) -> new ArrayList<>()).add(known);
        }
    }

    private static final Logger LOG = Logger.getInstance(ZigAttributes.class);
    private static final Set<Pair<String, Set<String>>> warnedUnknowns = new HashSet<>();
    private static void complainAboutUnknownCombo(String type, Set<String> modifiers) {
        val thePair = new Pair<>(type, modifiers);
        synchronized (warnedUnknowns) {
            if (warnedUnknowns.contains(thePair))
                return;
            warnedUnknowns.add(thePair);
            LOG.warn("Unrecognized semantic token! type " + type + ", modifiers: " + modifiers);
        }

    }
    public static Optional<TextAttributesKey> getKey(String type, Set<String> modifiers) {
        if (type == null) {
            return Optional.empty();
        }
        if (!types.containsKey(type)) {
            complainAboutUnknownCombo(type, modifiers);
            return Optional.empty();
        }
        val values = types.get(type);
        for (var known : values) {
            if ((modifiers != null && modifiers.equals(known.modifiers)) ||
                (modifiers == null && known.modifiers.isEmpty())) {
                return Optional.of(known.KEY);
            }
        }
        complainAboutUnknownCombo(type, modifiers);
        //Fallback with weaker matching
        for (var known : values) {
            if ((modifiers != null && modifiers.containsAll(known.modifiers)) ||
                (modifiers == null && known.modifiers.isEmpty())) {
                return Optional.of(known.KEY);
            }
        }
        return Optional.empty();
    }
}
