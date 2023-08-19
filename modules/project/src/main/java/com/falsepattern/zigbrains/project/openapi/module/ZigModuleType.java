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

package com.falsepattern.zigbrains.project.openapi.module;

import com.falsepattern.zigbrains.zig.Icons;
import com.falsepattern.zigbrains.project.ide.util.projectwizard.ZigModuleBuilder;
import com.intellij.openapi.module.ModuleType;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public class ZigModuleType extends ModuleType<ZigModuleBuilder> {
    public static final ZigModuleType INSTANCE = new ZigModuleType();

    public ZigModuleType() {
        super("com.falsepattern.zigbrains.zigModuleType");
    }

    @Override
    public @NotNull ZigModuleBuilder createModuleBuilder() {
        return new ZigModuleBuilder();
    }

    @Override
    public @NotNull @Nls(capitalization = Nls.Capitalization.Title) String getName() {
        return "Zig";
    }

    @Override
    public @NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String getDescription() {
        return "Zig module";
    }

    @Override
    public @NotNull Icon getNodeIcon(boolean isOpened) {
        return Icons.ZIG;
    }
}
