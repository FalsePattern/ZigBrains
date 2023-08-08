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

package com.falsepattern.zigbrains.zig.ide;

import com.falsepattern.zigbrains.common.Icons;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.PlainSyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Map;

public class ZigColorSettingsPage implements ColorSettingsPage {

    private static final AttributesDescriptor[] DESCRIPTORS =
            new AttributesDescriptor[]{new AttributesDescriptor("Builtin", ZigAttributes.Builtin.KEY),
                                       new AttributesDescriptor("Comment", ZigAttributes.Comment.KEY),
                                       new AttributesDescriptor("Enum", ZigAttributes.Enum.KEY),
                                       new AttributesDescriptor("Enum member", ZigAttributes.EnumMember.KEY),
                                       new AttributesDescriptor("Error tag", ZigAttributes.ErrorTag.KEY),
                                       new AttributesDescriptor("Field", ZigAttributes.Field.KEY),
                                       new AttributesDescriptor("Function", ZigAttributes.Function.KEY),
                                       new AttributesDescriptor("Keyword//Regular", ZigAttributes.Keyword.KEY),
                                       new AttributesDescriptor("Keyword//Literal", ZigAttributes.KeywordLiteral.KEY),
                                       new AttributesDescriptor("Label", ZigAttributes.Label.KEY),
                                       new AttributesDescriptor("Namespace", ZigAttributes.Namespace.KEY),
                                       new AttributesDescriptor("Number", ZigAttributes.Number.KEY),
                                       new AttributesDescriptor("Operator", ZigAttributes.Operator.KEY),
                                       new AttributesDescriptor("Parameter", ZigAttributes.Parameter.KEY),
                                       new AttributesDescriptor("String", ZigAttributes.String.KEY),
                                       new AttributesDescriptor("Struct", ZigAttributes.Struct.KEY),
                                       new AttributesDescriptor("Type", ZigAttributes.Type.KEY),
                                       new AttributesDescriptor("Variable", ZigAttributes.Variable.KEY)};

    @Nullable
    @Override
    public Icon getIcon() {
        return Icons.ZIG;
    }

    @NotNull
    @Override
    public SyntaxHighlighter getHighlighter() {
        return new PlainSyntaxHighlighter();
    }

    @NotNull
    @Override
    public String getDemoText() {
        return "Preview not yet implemented :/";
    }

    @Nullable
    @Override
    public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return null;
    }

    @Override
    public AttributesDescriptor @NotNull [] getAttributeDescriptors() {
        return DESCRIPTORS;
    }

    @Override
    public ColorDescriptor @NotNull [] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "Zig";
    }
}
