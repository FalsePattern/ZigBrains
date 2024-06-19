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

package com.falsepattern.zigbrains.zig.highlighter;

import com.falsepattern.zigbrains.zig.Icons;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.HashMap;
import java.util.Map;

public class ZigColorSettingsPage implements ColorSettingsPage {

    private static final AttributesDescriptor[] DESCRIPTORS =
            new AttributesDescriptor[]{
                    new AttributesDescriptor("Bad character", ZigSyntaxHighlighter.BAD_CHAR),
                    new AttributesDescriptor("Builtin", ZigSyntaxHighlighter.BUILTIN),
                    new AttributesDescriptor("Character literal", ZigSyntaxHighlighter.CHAR),
                    new AttributesDescriptor("Comment//Regular", ZigSyntaxHighlighter.COMMENT),
                    new AttributesDescriptor("Comment//Documentation", ZigSyntaxHighlighter.COMMENT_DOC),
                    new AttributesDescriptor("Enum//Reference", ZigSyntaxHighlighter.ENUM_REF),
                    new AttributesDescriptor("Enum//Declaration", ZigSyntaxHighlighter.ENUM_DECL),
                    new AttributesDescriptor("Enum//Member//Declaration", ZigSyntaxHighlighter.ENUM_MEMBER_DECL),
                    new AttributesDescriptor("Enum//Member//Reference", ZigSyntaxHighlighter.ENUM_MEMBER_REF),
                    new AttributesDescriptor("Error tag//Declaration", ZigSyntaxHighlighter.ERROR_TAG_DECL),
                    new AttributesDescriptor("Error tag//Reference", ZigSyntaxHighlighter.ERROR_TAG_REF),
                    new AttributesDescriptor("Function//Declaration", ZigSyntaxHighlighter.FUNCTION_DECL),
                    new AttributesDescriptor("Function//Declaration//Generic", ZigSyntaxHighlighter.FUNCTION_DECL_GEN),
                    new AttributesDescriptor("Function//Reference", ZigSyntaxHighlighter.FUNCTION_REF),
                    new AttributesDescriptor("Function//Reference//Generic", ZigSyntaxHighlighter.FUNCTION_REF_GEN),
                    new AttributesDescriptor("Keyword", ZigSyntaxHighlighter.KEYWORD),
                    new AttributesDescriptor("Label//Declaration", ZigSyntaxHighlighter.LABEL_REF),
                    new AttributesDescriptor("Label//Reference", ZigSyntaxHighlighter.LABEL_REF),
                    new AttributesDescriptor("Method//Declaration", ZigSyntaxHighlighter.METHOD_DECL),
                    new AttributesDescriptor("Method//Declaration//Generic", ZigSyntaxHighlighter.METHOD_DECL_GEN),
                    new AttributesDescriptor("Method//Reference", ZigSyntaxHighlighter.METHOD_REF),
                    new AttributesDescriptor("Method//Reference//Generic", ZigSyntaxHighlighter.METHOD_REF_GEN),
                    new AttributesDescriptor("Namespace//Declaration", ZigSyntaxHighlighter.NAMESPACE_DECL),
                    new AttributesDescriptor("Namespace//Reference", ZigSyntaxHighlighter.NAMESPACE_REF),
                    new AttributesDescriptor("Number", ZigSyntaxHighlighter.NUMBER),
                    new AttributesDescriptor("Operator", ZigSyntaxHighlighter.OPERATOR),
                    new AttributesDescriptor("Parameter", ZigSyntaxHighlighter.PARAMETER),
                    new AttributesDescriptor("Property//Declaration", ZigSyntaxHighlighter.PROPERTY_DECL),
                    new AttributesDescriptor("Property//Reference", ZigSyntaxHighlighter.PROPERTY_REF),
                    new AttributesDescriptor("String", ZigSyntaxHighlighter.STRING),
                    new AttributesDescriptor("Struct//Declaration", ZigSyntaxHighlighter.STRUCT_DECL),
                    new AttributesDescriptor("Struct//Reference", ZigSyntaxHighlighter.STRUCT_REF),
                    new AttributesDescriptor("Type//Declaration", ZigSyntaxHighlighter.TYPE_DECL),
                    new AttributesDescriptor("Type//Declaration//Generic", ZigSyntaxHighlighter.TYPE_DECL_GEN),
                    new AttributesDescriptor("Type//Reference", ZigSyntaxHighlighter.TYPE_REF),
                    new AttributesDescriptor("Type//Reference//Generic", ZigSyntaxHighlighter.TYPE_REF_GEN),
                    new AttributesDescriptor("Type parameter//Reference", ZigSyntaxHighlighter.TYPE_PARAM),
                    new AttributesDescriptor("Type parameter//Declaration", ZigSyntaxHighlighter.TYPE_PARAM_DECL),
                    new AttributesDescriptor("Variable//Declaration", ZigSyntaxHighlighter.VARIABLE_DECL),
                    new AttributesDescriptor("Variable//Declaration//Deprecated", ZigSyntaxHighlighter.VARIABLE_DECL_DEPR),
                    new AttributesDescriptor("Variable//Reference", ZigSyntaxHighlighter.VARIABLE_REF),
                    new AttributesDescriptor("Variable//Reference//Deprecated", ZigSyntaxHighlighter.VARIABLE_REF_DEPR),
                    };

    @Nullable
    @Override
    public Icon getIcon() {
        return Icons.ZIG;
    }

    @NotNull
    @Override
    public SyntaxHighlighter getHighlighter() {
        return new ZigSyntaxHighlighter();
    }

    @NotNull
    @Override
    public String getDemoText() {
        return """
                ///This is a documentation comment
                const <ns_decl>std</ns_decl> = @import("std");
                
                const <enum_decl>AnEnum</enum_decl> = enum {
                    <enum_member_decl>A</enum_member_decl>,
                    <enum_member_decl>B</enum_member_decl>,
                    <enum_member_decl>C</enum_member_decl>,
                };
                
                const <struct_decl>AStruct</struct_decl> = struct {
                    <property_decl>fieldA</property_decl>: <type>u32</type>,
                    <property_decl>fieldB</property_decl>: <type>u16</type>
                };
                
                const <type_decl>AnError</type_decl> = error {
                    <error_tag_decl>SomeError</error_tag_decl>
                };
                
                pub fn <function_decl>main</function_decl>() <type>AnError</type>.<error_tag>SomeError</error_tag>!<type>void</type> {
                    // Prints to stderr (it's a shortcut based on `std.io.getStdErr()`)
                    <namespace>std</namespace>.<namespace>debug</namespace>.<function>print</function>("All your {s} are belong to us.\\n", .{"codebase"});
                
                    // stdout is for the actual output of your application, for example if you
                    // are implementing gzip, then only the compressed bytes should be sent to
                    // stdout, not any debugging messages.
                    const <variable_decl>stdout_file</variable_decl> = <namespace>std</namespace>.<namespace>io</namespace>.<function>getStdOut</function>().<method>writer</method>();
                    var <variable_decl>bw</variable_decl> = <namespace>std</namespace>.<namespace>io</namespace>.<function>bufferedWriter</function>(<variable>stdout_file</variable>);
                    const <variable_decl>stdout</variable_decl> = <variable>bw</variable>.<method>writer</method>();
                
                    try <variable>stdout</variable>.<method>print</method>(\\\\Run `zig build test` to run the tests.
                                                         \\\\
                                                         , .{});
                
                    _ = <enum>AnEnum</enum>.<enum_member>A</enum_member>;
                
                    try <variable>bw</variable>.<method>flush</method>(); // don't forget to flush!
                }
                
                test "simple test" {
                    var <variable_decl>list</variable_decl> = <namespace>std</namespace>.<type>ArrayList</type>(<type>i32</type>).<function>init</function>(<namespace>std</namespace>.<namespace>testing</namespace>.<variable>allocator</variable>);
                    defer <variable>list</variable>.<method>deinit</method>(); // try commenting this out and see if zig detects the memory leak!
                    try <variable>list</variable>.<method>append</method>(42);
                    try <namespace>std</namespace>.<namespace>testing</namespace>.<method_gen>expectEqual</method_gen>(@as(<type>i32</type>, 42), <variable>list</variable>.<method>pop</method>());
                }
                """;
    }

    private static final Map<String, TextAttributesKey> ADD_HIGHLIGHT = new HashMap<>();
    static {
        ADD_HIGHLIGHT.put("enum", ZigSyntaxHighlighter.ENUM_REF);
        ADD_HIGHLIGHT.put("enum_decl", ZigSyntaxHighlighter.ENUM_DECL);
        ADD_HIGHLIGHT.put("enum_member", ZigSyntaxHighlighter.ENUM_MEMBER_REF);
        ADD_HIGHLIGHT.put("enum_member_decl", ZigSyntaxHighlighter.ENUM_MEMBER_DECL);
        ADD_HIGHLIGHT.put("error_tag", ZigSyntaxHighlighter.ERROR_TAG_REF);
        ADD_HIGHLIGHT.put("error_tag_decl", ZigSyntaxHighlighter.ERROR_TAG_DECL);
        ADD_HIGHLIGHT.put("function", ZigSyntaxHighlighter.FUNCTION_REF);
        ADD_HIGHLIGHT.put("function_decl", ZigSyntaxHighlighter.FUNCTION_DECL);
        ADD_HIGHLIGHT.put("method", ZigSyntaxHighlighter.METHOD_REF);
        ADD_HIGHLIGHT.put("method_gen", ZigSyntaxHighlighter.METHOD_REF_GEN);
        ADD_HIGHLIGHT.put("namespace", ZigSyntaxHighlighter.NAMESPACE_REF);
        ADD_HIGHLIGHT.put("property_decl", ZigSyntaxHighlighter.PROPERTY_DECL);
        ADD_HIGHLIGHT.put("struct", ZigSyntaxHighlighter.STRUCT_REF);
        ADD_HIGHLIGHT.put("struct_decl", ZigSyntaxHighlighter.STRUCT_DECL);
        ADD_HIGHLIGHT.put("type", ZigSyntaxHighlighter.TYPE_REF);
        ADD_HIGHLIGHT.put("type_decl", ZigSyntaxHighlighter.TYPE_DECL);
        ADD_HIGHLIGHT.put("variable", ZigSyntaxHighlighter.VARIABLE_REF);
        ADD_HIGHLIGHT.put("variable_decl", ZigSyntaxHighlighter.VARIABLE_DECL);
    }

    @Nullable
    @Override
    public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return ADD_HIGHLIGHT;
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
