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
import com.falsepattern.zigbrains.project.execution.base.ZigConfigEditor.ArgsConfigurable;
import com.falsepattern.zigbrains.project.execution.base.ZigConfigEditor.CheckboxConfigurable;
import com.falsepattern.zigbrains.project.execution.base.ZigConfigEditor.FilePathConfigurable;
import com.falsepattern.zigbrains.project.execution.base.ZigConfigEditor.OptimizationConfigurable;
import com.falsepattern.zigbrains.project.execution.base.ZigConfigEditor.ZigConfigurable;
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

import static com.falsepattern.zigbrains.project.execution.base.ZigConfigEditor.coloredConfigurable;

@Getter
public class ZigExecConfigRun extends ZigExecConfigBase<ZigExecConfigRun> {
    private FilePathConfigurable filePath = new FilePathConfigurable("filePath", "File Path");
    private CheckboxConfigurable colored = coloredConfigurable("colored");
    private OptimizationConfigurable optimization = new OptimizationConfigurable("optimization");
    private ArgsConfigurable compilerArgs = new ArgsConfigurable("compilerArgs", "Extra compiler command line arguments");
    private ArgsConfigurable exeArgs = new ArgsConfigurable("exeArgs", "Output program command line arguments");
    public ZigExecConfigRun(@NotNull Project project, @NotNull ConfigurationFactory factory) {
        super(project, factory, "Zig Run");
    }

    @Override
    public List<String> buildCommandLineArgs(boolean debug) {
        val result = new ArrayList<String>();
        result.add(debug ? "build-exe" : "run");
        result.addAll(CLIUtil.colored(colored.value, debug));
        result.add(filePath.getPathOrThrow().toString());
        if (!debug || optimization.forced) {
            result.addAll(List.of("-O", optimization.level.name()));
        }
        result.addAll(List.of(compilerArgs.args));
        if (!debug) {
            result.add("--");
            result.addAll(List.of(exeArgs.args));
        }
        return result;
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
        clone.compilerArgs = compilerArgs.clone();
        clone.optimization = optimization.clone();
        clone.exeArgs = exeArgs.clone();
        return clone;
    }

    @Override
    public @NotNull List<ZigConfigurable<?>> getConfigurables() {
        return CollectionUtil.concat(super.getConfigurables(), filePath, optimization, colored, compilerArgs, exeArgs);
    }

    @Override
    public @Nullable ProfileStateRun getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
        return new ProfileStateRun(environment, this);
    }
}
