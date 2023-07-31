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

package com.falsepattern.zigbrains.ide;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import org.jetbrains.annotations.Unmodifiable;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static com.intellij.openapi.editor.DefaultLanguageHighlighterColors.CLASS_NAME;
import static com.intellij.openapi.editor.DefaultLanguageHighlighterColors.CLASS_REFERENCE;
import static com.intellij.openapi.editor.DefaultLanguageHighlighterColors.FUNCTION_DECLARATION;
import static com.intellij.openapi.editor.DefaultLanguageHighlighterColors.GLOBAL_VARIABLE;
import static com.intellij.openapi.editor.DefaultLanguageHighlighterColors.KEYWORD;
import static com.intellij.openapi.editor.DefaultLanguageHighlighterColors.LABEL;
import static com.intellij.openapi.editor.DefaultLanguageHighlighterColors.LINE_COMMENT;
import static com.intellij.openapi.editor.DefaultLanguageHighlighterColors.LOCAL_VARIABLE;
import static com.intellij.openapi.editor.DefaultLanguageHighlighterColors.NUMBER;
import static com.intellij.openapi.editor.DefaultLanguageHighlighterColors.OPERATION_SIGN;
import static com.intellij.openapi.editor.DefaultLanguageHighlighterColors.PARAMETER;
import static com.intellij.openapi.editor.DefaultLanguageHighlighterColors.STATIC_FIELD;
import static com.intellij.openapi.editor.DefaultLanguageHighlighterColors.STATIC_METHOD;
import static com.intellij.openapi.editor.DefaultLanguageHighlighterColors.STRING;

public enum ZigAttributes {
    Type(CLASS_NAME),
    Parameter(PARAMETER),
    Variable(LOCAL_VARIABLE),
    Enum(GLOBAL_VARIABLE),
    EnumMember(GLOBAL_VARIABLE),
    Field(STATIC_FIELD),
    ErrorTag(CLASS_REFERENCE),
    Function(FUNCTION_DECLARATION),
    Keyword(KEYWORD),
    Comment(LINE_COMMENT),
    String(STRING),
    Number(NUMBER),
    Operator(OPERATION_SIGN),
    Builtin(STATIC_METHOD),
    Label(LABEL),
    KeywordLiteral(Keyword.KEY),
    Namespace(CLASS_NAME),
    Struct(CLASS_NAME);
    public final TextAttributesKey KEY;
    public final String type;
    public final @Unmodifiable Set<String> modifiers;

    ZigAttributes(TextAttributesKey defaultKey, String... modifiers) {
        var name = name();
        int underscoreIndex = name.indexOf('_');
        type = (underscoreIndex >= 0 ? name.substring(0, underscoreIndex) : name).toLowerCase(Locale.ROOT);
        KEY = TextAttributesKey.createTextAttributesKey("ZIG_" + name.toUpperCase(Locale.ROOT), defaultKey);
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
        if (modifiers != null && modifiers.size() > 0) {
            System.out.println(type + ": " + modifiers);
        }
        return Optional.empty();
    }
}
