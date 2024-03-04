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

package com.falsepattern.zigbrains.project.execution.binary;

import com.falsepattern.zigbrains.zig.Icons;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class ConfigTypeBinary extends ConfigurationTypeBase {
    public static final String IDENTIFIER = "ZIGBRAINS_BINARY";
    public ConfigTypeBinary() {
        super(IDENTIFIER, "Zig-compiled native executable", "Binary executable compiled from zig code", Icons.ZIG);
        addFactory(new ConfigFactoryBinary());
    }

    public class ConfigFactoryBinary extends ConfigurationFactory {
        public ConfigFactoryBinary() {
            super(ConfigTypeBinary.this);
        }

        @Override
        public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
            return new ZigExecConfigBinary(project, this);
        }

        @Override
        public @NotNull @NonNls String getId() {
            return IDENTIFIER;
        }
    }
}
