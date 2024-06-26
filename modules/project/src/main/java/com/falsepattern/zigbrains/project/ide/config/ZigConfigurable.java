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

package com.falsepattern.zigbrains.project.ide.config;

import com.falsepattern.zigbrains.common.MultiConfigurable;
import com.falsepattern.zigbrains.project.ide.project.ZigProjectConfigurable;
import com.falsepattern.zigbrains.zig.settings.ZLSSettingsConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.NotNull;

public class ZigConfigurable extends MultiConfigurable {
    public ZigConfigurable(@NotNull Project project) {
        super(new ZigProjectConfigurable(project), new ZLSSettingsConfigurable(project));
    }
    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "Zig";
    }
}
