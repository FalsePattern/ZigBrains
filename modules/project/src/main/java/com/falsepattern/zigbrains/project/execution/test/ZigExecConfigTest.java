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

package com.falsepattern.zigbrains.project.execution.test;

import com.falsepattern.zigbrains.project.execution.base.ProfileStateBase;
import com.falsepattern.zigbrains.project.execution.base.ZigExecConfigBase;
import com.falsepattern.zigbrains.project.execution.common.ZigConfigEditor;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ZigExecConfigTest extends ZigExecConfigBase<ZigExecConfigTest> {
    public String filePath = "";
    public ZigExecConfigTest(@NotNull Project project, @NotNull ConfigurationFactory factory) {
        super(project, factory, "Zig Test");
    }

    @Override
    public String[] buildCommandLineArgs() {
        return new String[]{"test", filePath};
    }

    @Override
    public @Nullable String suggestedName() {
        return "Test";
    }

    @Override
    public @Nullable ProfileStateBase<ZigExecConfigTest> getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
        return new ProfileStateTest(environment, this);
    }

    @Override
    public @NotNull SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new Editor();
    }

    public static class Editor extends ZigConfigEditor.WithFilePath<ZigExecConfigTest> {

        @Override
        protected String getFilePath(ZigExecConfigTest config) {
            return config.filePath;
        }

        @Override
        protected void setFilePath(ZigExecConfigTest config, String path) {
            config.filePath = path;
        }
    }
}
