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

package com.falsepattern.zigbrains.project.execution.run;

import com.falsepattern.zigbrains.project.execution.base.ZigExecConfigBase;
import com.falsepattern.zigbrains.project.execution.base.ZigConfigEditor;
import com.falsepattern.zigbrains.project.util.ElementUtil;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Setter
@Getter
public class ZigExecConfigRun extends ZigExecConfigBase<ZigExecConfigRun> {
    public String filePath = "";
    public ZigExecConfigRun(@NotNull Project project, @NotNull ConfigurationFactory factory) {
        super(project, factory, "Zig Run");
    }

    @Override
    public String[] buildCommandLineArgs() {
        return new String[]{"run", filePath};
    }

    @Override
    public @Nullable String suggestedName() {
        return "Run";
    }

    @Override
    public @NotNull Editor getConfigurationEditor() {
        return new Editor();
    }

    @Override
    public @Nullable ProfileStateRun getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
        return new ProfileStateRun(environment, this);
    }

    @Override
    public void readExternal(@NotNull Element element) throws InvalidDataException {
        super.readExternal(element);

        var filePath = ElementUtil.readString(element, "filePath");
        if (filePath != null) {
            this.filePath = filePath;
        }
    }

    @Override
    public void writeExternal(@NotNull Element element) {
        super.writeExternal(element);

        ElementUtil.writeString(element, "filePath", filePath);
    }

    public static class Editor extends ZigConfigEditor.WithFilePath<ZigExecConfigRun> {

        @Override
        protected String getFilePath(ZigExecConfigRun config) {
            return config.filePath;
        }

        @Override
        protected void setFilePath(ZigExecConfigRun config, String path) {
            config.filePath = path;
        }
    }
}
