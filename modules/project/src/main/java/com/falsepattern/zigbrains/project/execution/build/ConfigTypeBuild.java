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

package com.falsepattern.zigbrains.project.execution.build;

import com.falsepattern.zigbrains.zig.Icons;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class ConfigTypeBuild extends ConfigurationTypeBase {
    public static final String IDENTIFIER = "ZIGBRAINS_BUILD";

    public static ConfigTypeBuild getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(ConfigTypeBuild.class);
    }

    public ConfigTypeBuild() {
        super(IDENTIFIER, "ZigBuild", "Zig Build", Icons.ZIG);
        addFactory(new ConfigFactoryBuild());
    }

    public class ConfigFactoryBuild extends ConfigurationFactory {
        public ConfigFactoryBuild() {
            super(ConfigTypeBuild.this);
        }

        @Override
        public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
            return new ZigExecConfigBuild(project, this);
        }

        @Override
        public @NotNull @NonNls String getId() {
            return IDENTIFIER;
        }
    }
}
