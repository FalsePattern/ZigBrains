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

package com.falsepattern.zigbrains.zon.highlighting

import com.falsepattern.zigbrains.Icons
import com.falsepattern.zigbrains.ZigBrainsBundle
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage

class ZonColorSettingsPage: ColorSettingsPage {
    override fun getAttributeDescriptors() = DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName() = ZigBrainsBundle.message("configurable.name.zon-color-settings-page")

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
    AttributesDescriptor(ZigBrainsBundle.message("zon.color-settings.eq"), ZonSyntaxHighlighter.EQ),
    AttributesDescriptor(ZigBrainsBundle.message("zon.color-settings.id"), ZonSyntaxHighlighter.ID),
    AttributesDescriptor(ZigBrainsBundle.message("zon.color-settings.comment"), ZonSyntaxHighlighter.COMMENT),
    AttributesDescriptor(ZigBrainsBundle.message("zon.color-settings.bad_char"), ZonSyntaxHighlighter.BAD_CHAR),
    AttributesDescriptor(ZigBrainsBundle.message("zon.color-settings.string"), ZonSyntaxHighlighter.STRING),
    AttributesDescriptor(ZigBrainsBundle.message("zon.color-settings.comma"), ZonSyntaxHighlighter.COMMA),
    AttributesDescriptor(ZigBrainsBundle.message("zon.color-settings.dot"), ZonSyntaxHighlighter.DOT),
    AttributesDescriptor(ZigBrainsBundle.message("zon.color-settings.boolean"), ZonSyntaxHighlighter.BOOLEAN),
    AttributesDescriptor(ZigBrainsBundle.message("zon.color-settings.brace"), ZonSyntaxHighlighter.BRACE)
)