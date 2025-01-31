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

package com.falsepattern.zigbrains.zig.highlighter

import com.falsepattern.zigbrains.Icons
import com.falsepattern.zigbrains.ZigBrainsBundle
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import kotlinx.collections.immutable.toImmutableMap


class ZigColorSettingsPage: ColorSettingsPage {
    override fun getAttributeDescriptors() = DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName() = ZigBrainsBundle.message("configurable.name.zig-color-settings-page")

    override fun getIcon() = Icons.Zig

    override fun getHighlighter() = ZigSyntaxHighlighter()

    override fun getDemoText() = """
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
            <namespace>std</namespace>.<namespace>debug</namespace>.<function>print</function>("All your {s} are belong to us.\n", .{"codebase"});
        
            // stdout is for the actual output of your application, for example if you
            // are implementing gzip, then only the compressed bytes should be sent to
            // stdout, not any debugging messages.
            const <variable_decl>stdout_file</variable_decl> = <namespace>std</namespace>.<namespace>io</namespace>.<function>getStdOut</function>().<method>writer</method>();
            var <variable_decl>bw</variable_decl> = <namespace>std</namespace>.<namespace>io</namespace>.<function>bufferedWriter</function>(<variable>stdout_file</variable>);
            const <variable_decl>stdout</variable_decl> = <variable>bw</variable>.<method>writer</method>();
        
            try <variable>stdout</variable>.<method>print</method>(\\Run `zig build test` to run the tests.
                                                 \\
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
    """.trimIndent()

    override fun getAdditionalHighlightingTagToDescriptorMap() =
        ADD_HIGHLIGHT
}

private val ADD_HIGHLIGHT = HashMap<String, TextAttributesKey>().apply {
    this["enum"] = ZigSyntaxHighlighter.ENUM_REF
    this["enum_decl"] = ZigSyntaxHighlighter.ENUM_DECL
    this["enum_member"] = ZigSyntaxHighlighter.ENUM_MEMBER_REF
    this["enum_member_decl"] = ZigSyntaxHighlighter.ENUM_MEMBER_DECL
    this["error_tag"] = ZigSyntaxHighlighter.ERROR_TAG_REF
    this["error_tag_decl"] = ZigSyntaxHighlighter.ERROR_TAG_DECL
    this["function"] = ZigSyntaxHighlighter.FUNCTION_REF
    this["function_decl"] = ZigSyntaxHighlighter.FUNCTION_DECL
    this["method"] = ZigSyntaxHighlighter.METHOD_REF
    this["method_gen"] = ZigSyntaxHighlighter.METHOD_REF_GEN
    this["namespace"] = ZigSyntaxHighlighter.NAMESPACE_REF
    this["property_decl"] = ZigSyntaxHighlighter.PROPERTY_DECL
    this["struct"] = ZigSyntaxHighlighter.STRUCT_REF
    this["struct_decl"] = ZigSyntaxHighlighter.STRUCT_DECL
    this["type"] = ZigSyntaxHighlighter.TYPE_REF
    this["type_decl"] = ZigSyntaxHighlighter.TYPE_DECL
    this["variable"] = ZigSyntaxHighlighter.VARIABLE_REF
    this["variable_decl"] = ZigSyntaxHighlighter.VARIABLE_DECL
}.toImmutableMap()


private val DESCRIPTORS: Array<AttributesDescriptor> = arrayOf(
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.builtin"), ZigSyntaxHighlighter.BUILTIN),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.char"), ZigSyntaxHighlighter.CHAR),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.comment"), ZigSyntaxHighlighter.COMMENT),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.comment-doc"), ZigSyntaxHighlighter.COMMENT_DOC),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.enum-ref"), ZigSyntaxHighlighter.ENUM_REF),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.enum-decl"), ZigSyntaxHighlighter.ENUM_DECL),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.enum-member-decl"), ZigSyntaxHighlighter.ENUM_MEMBER_DECL),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.enum-member-ref"), ZigSyntaxHighlighter.ENUM_MEMBER_REF),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.error-tag-decl"), ZigSyntaxHighlighter.ERROR_TAG_DECL),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.error-tag-ref"), ZigSyntaxHighlighter.ERROR_TAG_REF),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.function-decl"), ZigSyntaxHighlighter.FUNCTION_DECL),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.function-decl-gen"), ZigSyntaxHighlighter.FUNCTION_DECL_GEN),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.function-ref"), ZigSyntaxHighlighter.FUNCTION_REF),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.function-ref-gen"), ZigSyntaxHighlighter.FUNCTION_REF_GEN),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.keyword"), ZigSyntaxHighlighter.KEYWORD),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.label-decl"), ZigSyntaxHighlighter.LABEL_DECL),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.label-ref"), ZigSyntaxHighlighter.LABEL_REF),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.method-decl"), ZigSyntaxHighlighter.METHOD_DECL),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.method-decl-gen"), ZigSyntaxHighlighter.METHOD_DECL_GEN),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.method-ref"), ZigSyntaxHighlighter.METHOD_REF),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.method-ref-gen"), ZigSyntaxHighlighter.METHOD_REF_GEN),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.namespace-decl"), ZigSyntaxHighlighter.NAMESPACE_DECL),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.namespace-ref"), ZigSyntaxHighlighter.NAMESPACE_REF),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.number"), ZigSyntaxHighlighter.NUMBER),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.operator"), ZigSyntaxHighlighter.OPERATOR),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.parameter"), ZigSyntaxHighlighter.PARAMETER),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.property-decl"), ZigSyntaxHighlighter.PROPERTY_DECL),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.property-ref"), ZigSyntaxHighlighter.PROPERTY_REF),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.string"), ZigSyntaxHighlighter.STRING),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.string-esc-v"), ZigSyntaxHighlighter.STRING_ESC_V),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.string-esc-i-c"), ZigSyntaxHighlighter.STRING_ESC_I_C),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.string-esc-i-u"), ZigSyntaxHighlighter.STRING_ESC_I_U),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.struct-decl"), ZigSyntaxHighlighter.STRUCT_DECL),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.struct-ref"), ZigSyntaxHighlighter.STRUCT_REF),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.type-decl"), ZigSyntaxHighlighter.TYPE_DECL),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.type-decl-gen"), ZigSyntaxHighlighter.TYPE_DECL_GEN),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.type-ref"), ZigSyntaxHighlighter.TYPE_REF),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.type-ref-gen"), ZigSyntaxHighlighter.TYPE_REF_GEN),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.type-param"), ZigSyntaxHighlighter.TYPE_PARAM),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.type-param-decl"), ZigSyntaxHighlighter.TYPE_PARAM_DECL),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.variable-decl"), ZigSyntaxHighlighter.VARIABLE_DECL),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.variable-decl-depr"), ZigSyntaxHighlighter.VARIABLE_DECL_DEPR),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.variable-ref"), ZigSyntaxHighlighter.VARIABLE_REF),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.variable-ref-depr"), ZigSyntaxHighlighter.VARIABLE_REF_DEPR),
    AttributesDescriptor(ZigBrainsBundle.message("zig.color-settings.bad-char"), ZigSyntaxHighlighter.BAD_CHAR),
)