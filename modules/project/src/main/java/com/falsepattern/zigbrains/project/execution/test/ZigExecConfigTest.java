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

import com.falsepattern.zigbrains.common.util.CollectionUtil;
import com.falsepattern.zigbrains.project.execution.base.ProfileStateBase;
import com.falsepattern.zigbrains.project.execution.base.ZigConfigEditor;
import com.falsepattern.zigbrains.project.execution.base.ZigExecConfigBase;
import com.falsepattern.zigbrains.project.util.CLIUtil;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ZigExecConfigTest extends ZigExecConfigBase<ZigExecConfigTest> {
    private ZigConfigEditor.FilePathConfigurable filePath = new ZigConfigEditor.FilePathConfigurable("filePath", "File path");
    private ZigConfigEditor.CheckboxConfigurable colored = ZigConfigEditor.coloredConfigurable("colored");
    private ZigConfigEditor.OptimizationConfigurable optimization = new ZigConfigEditor.OptimizationConfigurable("optimization");
    private ZigConfigEditor.ArgsConfigurable compilerArgs = new ZigConfigEditor.ArgsConfigurable("compilerArgs", "Extra compiler command line arguments");
    public ZigExecConfigTest(@NotNull Project project, @NotNull ConfigurationFactory factory) {
        super(project, factory, "Zig Test");
    }

    @Override
    public List<String> buildCommandLineArgs(boolean debug) {
        val result = new ArrayList<String>();
        result.add("test");
        result.addAll(CLIUtil.colored(colored.value, debug));
        result.add(filePath.getPathOrThrow().toString());
        if (!debug || optimization.forced) {
            result.addAll(List.of("-O", optimization.level.name()));
        }
        result.addAll(List.of(compilerArgs.args));
        if (debug) {
            result.add("--test-no-exec");
        }
        return result;
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
    public ZigExecConfigTest clone() {
        val clone = super.clone();
        clone.filePath = filePath.clone();
        clone.colored = colored.clone();
        clone.optimization = optimization.clone();
        clone.compilerArgs = compilerArgs.clone();
        return clone;
    }

    @Override
    public @NotNull List<ZigConfigEditor.ZigConfigurable<?>> getConfigurables() {
        return CollectionUtil.concat(super.getConfigurables(), filePath, optimization, colored, compilerArgs);
    }
}
