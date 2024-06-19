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

package com.falsepattern.zigbrains.zon.highlight;

import com.falsepattern.zigbrains.zon.Icons;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Map;

public class ZonColorSettingsPage implements ColorSettingsPage {
    private static final AttributesDescriptor[] DESCRIPTORS =
            new AttributesDescriptor[]{desc("Equals", ZonSyntaxHighlighter.EQ),
                                       desc("Identifier", ZonSyntaxHighlighter.ID),
                                       desc("Comment", ZonSyntaxHighlighter.COMMENT),
                                       desc("Bad Value", ZonSyntaxHighlighter.BAD_CHAR),
                                       desc("String", ZonSyntaxHighlighter.STRING),
                                       desc("Comma", ZonSyntaxHighlighter.COMMA), desc("Dot", ZonSyntaxHighlighter.DOT),
                                       desc("Braces", ZonSyntaxHighlighter.BRACE)};

    private static AttributesDescriptor desc(String name, TextAttributesKey key) {
        return new AttributesDescriptor(name, key);
    }

    @Override
    public @Nullable Icon getIcon() {
        return Icons.ZON;
    }

    @Override
    public @NotNull SyntaxHighlighter getHighlighter() {
        return new ZonSyntaxHighlighter();
    }

    @Override
    public @NonNls @NotNull String getDemoText() {
        return """
                .{
                    //This is an example file with some random data
                    .name = "zls",
                    .version = "0.11.0",
                               
                    .dependencies = .{
                        .known_folders = .{
                            .url = "https://github.com/ziglibs/known-folders/archive/fa75e1bc672952efa0cf06160bbd942b47f6d59b.tar.gz",
                            .hash = "122048992ca58a78318b6eba4f65c692564be5af3b30fbef50cd4abeda981b2e7fa5",
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
                }
                """;
    }

    @Override
    public @Nullable Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return null;
    }

    @Override
    public @NotNull AttributesDescriptor[] getAttributeDescriptors() {
        return DESCRIPTORS;
    }

    @Override
    public @NotNull ColorDescriptor[] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Zon";
    }
}
