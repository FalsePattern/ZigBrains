package com.falsepattern.zigbrains.zon.highlighting

import com.falsepattern.zigbrains.zon.Icons
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage

class ZonColorSettingsPage: ColorSettingsPage {
    override fun getAttributeDescriptors() = DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName() = "Zon"

    override fun getIcon() = Icons.ZON

    override fun getHighlighter() = ZonSyntaxHighlighter()

    override fun getDemoText() = """
        .{
            //This is an example file with some random data
            .name = "zls",
            .version = "0.11.0",
        
            .dependencies = .{
                .known_folders = .{
                    .url = "https://github.com/ziglibs/known-folders/archive/fa75e1bc672952efa0cf06160bbd942b47f6d59b.tar.gz",
                    .hash = "122048992ca58a78318b6eba4f65c692564be5af3b30fbef50cd4abeda981b2e7fa5",
                    .lazy = true,
                },
                .diffz = .{
                    .url = "https://github.com/ziglibs/diffz/archive/90353d401c59e2ca5ed0abe5444c29ad3d7489aa.tar.gz",
                    .hash = "122089a8247a693cad53beb161bde6c30f71376cd4298798d45b32740c3581405864",
                },
                .binned_allocator = .{
                    .url = "https://gist.github.com/antlilja/8372900fcc09e38d7b0b6bbaddad3904/archive/6c3321e0969ff2463f8335da5601986cf2108690.tar.gz",
                    .hash = "1220363c7e27b2d3f39de6ff6e90f9537a0634199860fea237a55ddb1e1717f5d6a5",
                },
            },
            .paths = .{""},
        }
    """.trimIndent()

    override fun getAdditionalHighlightingTagToDescriptorMap() = null
}

val DESCRIPTORS = arrayOf(
    AttributesDescriptor("Equals", ZonSyntaxHighlighter.EQ),
    AttributesDescriptor("Identifier", ZonSyntaxHighlighter.ID),
    AttributesDescriptor("Comment", ZonSyntaxHighlighter.COMMENT),
    AttributesDescriptor("Bad value", ZonSyntaxHighlighter.BAD_CHAR),
    AttributesDescriptor("String", ZonSyntaxHighlighter.STRING),
    AttributesDescriptor("Comma", ZonSyntaxHighlighter.COMMA),
    AttributesDescriptor("Dot", ZonSyntaxHighlighter.DOT),
    AttributesDescriptor("Boolean", ZonSyntaxHighlighter.BOOLEAN),
    AttributesDescriptor("Braces", ZonSyntaxHighlighter.BRACE)
)