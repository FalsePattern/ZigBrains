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
                const <ns>std</ns> = @import("std");
                               
                const <enumDecl>AnEnum</enumDecl> = enum {
                    <enumMemberDecl>A</enumMemberDecl>,
                    <enumMemberDecl>B</enumMemberDecl>,
                    <enumMemberDecl>C</enumMemberDecl>,
                };
                               
                const <structDecl>AStruct</structDecl> = struct {
                    <propertyDecl>fieldA</propertyDecl>: <type>u32</type>,
                    <propertyDecl>fieldB</propertyDecl>: <type>u16</type>
                };
                               
                const <typeDecl>AnErrorType</typeDecl> = error {
                    <etagDecl>SomeError</etagDecl>
                };
                                
                pub fn <fn>main</fn>() <type>AnError</type>.SomeError!<type>void</type> {
                    // Prints to stderr (it's a shortcut based on `std.io.getStdErr()`)
                    <ns>std</ns>.<ns>debug</ns>.<fn>print</fn>("All your {s} are belong to us.\\n", .{"codebase"});
                                
                    // stdout is for the actual output of your application, for example if you
                    // are implementing gzip, then only the compressed bytes should be sent to
                    // stdout, not any debugging messages.
                    const <varDecl>stdout_file</varDecl> = <ns>std</ns>.<ns>io</ns>.<fn>getStdOut</fn>().<md>writer</md>();
                    var <varDecl>bw</varDecl> = <ns>std</ns>.<ns>io</ns>.<fn>bufferedWriter</fn>(<var>stdout_file</var>);
                    const <varDecl>stdout</varDecl> = <var>bw</var>.<md>writer</md>();
                                
                    try <var>stdout</var>.<md>print</md>(\\\\Run `zig build test` to run the tests.
                                                         \\\\
                                                         , .{});
                    
                    _ = <enum>AnEnum</enum>.<enumMember>A</enumMember>;
                                
                    try <var>bw</var>.<md>flush</md>(); // don't forget to flush!
                }
                                
                test "simple test" {
                    var <varDecl>list</varDecl> = <ns>std</ns>.<type>ArrayList</type>(<type>i32</type>).<md>init</md>(<ns>std</ns>.<ns>testing</ns>.<type>allocator</type>);
                    defer <var>list</var>.<md>deinit</md>(); // try commenting this out and see if zig detects the memory leak!
                    try <var>list</var>.<md>append</md>(42);
                    try <ns>std</ns>.<ns>testing</ns>.<fn>expectEqual</fn>(@as(i32, 42), <var>list</var>.<md>pop</md>());
                }
                """;
    }

    private static final Map<String, TextAttributesKey> ADD_HIGHLIGHT = new HashMap<>();
    static {
        ADD_HIGHLIGHT.put("typeDecl", ZigSyntaxHighlighter.TYPE_DECL);
        ADD_HIGHLIGHT.put("etagDecl", ZigSyntaxHighlighter.ERROR_TAG_DECL);
        ADD_HIGHLIGHT.put("struct", ZigSyntaxHighlighter.STRUCT_REF);
        ADD_HIGHLIGHT.put("enum", ZigSyntaxHighlighter.ENUM_REF);
        ADD_HIGHLIGHT.put("enumDecl", ZigSyntaxHighlighter.ENUM_DECL);
        ADD_HIGHLIGHT.put("enumMember", ZigSyntaxHighlighter.ENUM_MEMBER_REF);
        ADD_HIGHLIGHT.put("enumMemberDecl", ZigSyntaxHighlighter.ENUM_MEMBER_DECL);
        ADD_HIGHLIGHT.put("varDecl", ZigSyntaxHighlighter.VARIABLE_DECL);
        ADD_HIGHLIGHT.put("propertyDecl", ZigSyntaxHighlighter.PROPERTY_DECL);
        ADD_HIGHLIGHT.put("structDecl", ZigSyntaxHighlighter.STRUCT_DECL);
        ADD_HIGHLIGHT.put("var", ZigSyntaxHighlighter.VARIABLE_REF);
        ADD_HIGHLIGHT.put("ns", ZigSyntaxHighlighter.NAMESPACE_REF);
        ADD_HIGHLIGHT.put("type", ZigSyntaxHighlighter.TYPE_REF);
        ADD_HIGHLIGHT.put("fn", ZigSyntaxHighlighter.FUNCTION_REF);
        ADD_HIGHLIGHT.put("md", ZigSyntaxHighlighter.METHOD_REF);
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
