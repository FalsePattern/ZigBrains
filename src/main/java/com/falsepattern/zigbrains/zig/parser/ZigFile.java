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

package com.falsepattern.zigbrains.zig.parser;

import com.falsepattern.zigbrains.zig.ide.ZigFileType;
import com.falsepattern.zigbrains.zig.ide.ZigLanguage;
import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;

public class ZigFile extends PsiFileBase {
    public ZigFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, ZigLanguage.INSTANCE);
    }

    @Override
    public @NotNull FileType getFileType() {
        return ZigFileType.INSTANCE;
    }

    @Override
    public String toString() {
        return "Zig File";
    }
}
