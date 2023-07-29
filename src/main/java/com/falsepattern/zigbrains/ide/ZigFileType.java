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

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public class ZigFileType extends LanguageFileType {
    public static final ZigFileType INSTANCE = new ZigFileType();

    private ZigFileType() {
        super(ZigLanguage.INSTANCE);
    }

    @Override
    public @NonNls @NotNull String getName() {
        return "Zig File";
    }

    @Override
    public @NotNull String getDescription() {
        return "ZigLang file";
    }

    @Override
    public @NotNull String getDefaultExtension() {
        return "zig";
    }

    @Override
    public Icon getIcon() {
        return ZigIcons.FILE;
    }
}
