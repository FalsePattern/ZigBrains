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

import com.falsepattern.zigbrains.common.util.CollectionUtil;
import com.falsepattern.zigbrains.project.execution.base.ZigConfigEditor;
import com.falsepattern.zigbrains.project.execution.base.ZigExecConfigBase;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Getter
public class ZigExecConfigRun extends ZigExecConfigBase<ZigExecConfigRun> {
    private ZigConfigEditor.FilePathConfigurable filePath = new ZigConfigEditor.FilePathConfigurable("filePath", "File Path");
    private ZigConfigEditor.CheckboxConfigurable colored = ZigConfigEditor.coloredConfigurable("colored");
    private ZigConfigEditor.OptimizationConfigurable optimization = new ZigConfigEditor.OptimizationConfigurable("optimization");
    private ZigConfigEditor.ArgsConfigurable exeArgs = new ZigConfigEditor.ArgsConfigurable("exeArgs", "Arguments for the compile exe");
    public ZigExecConfigRun(@NotNull Project project, @NotNull ConfigurationFactory factory) {
        super(project, factory, "Zig Run");
    }

    @Override
    public String[] buildCommandLineArgs() {
        return CollectionUtil.concat(new String[]{"run", "--color", colored.value ? "on" : "off", filePath.getPathOrThrow().toString(), "-O", optimization.level.name(), "--"}, exeArgs.args).toArray(String[]::new);
    }

    @Override
    public String[] buildDebugCommandLineArgs() {
        if (optimization.forced) {
            return new String[]{"build-exe", "--color", colored.value ? "on" : "off", filePath.getPathOrThrow().toString(), "-O", optimization.level.name()};
        } else {
            return new String[]{"build-exe", "--color", colored.value ? "on" : "off", filePath.getPathOrThrow().toString()};
        }
    }

    @Override
    public @Nullable String suggestedName() {
        return "Run";
    }

    @Override
    public ZigExecConfigRun clone() {
        val clone = super.clone();
        clone.filePath = filePath.clone();
        clone.colored = colored.clone();
        clone.optimization = optimization.clone();
        clone.exeArgs = exeArgs.clone();
        return clone;
    }

    @Override
    public @NotNull List<ZigConfigEditor.ZigConfigurable<?>> getConfigurables() {
        return CollectionUtil.concat(super.getConfigurables(), filePath, optimization, colored);
    }

    @Override
    public @Nullable ProfileStateRun getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
        return new ProfileStateRun(environment, this);
    }
}
