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

import com.falsepattern.zigbrains.common.ZBFeatures;
import com.falsepattern.zigbrains.common.util.CollectionUtil;
import com.falsepattern.zigbrains.project.execution.base.ZigConfigEditor.ArgsConfigurable;
import com.falsepattern.zigbrains.project.execution.base.ZigConfigEditor.CheckboxConfigurable;
import com.falsepattern.zigbrains.project.execution.base.ZigConfigEditor.FilePathConfigurable;
import com.falsepattern.zigbrains.project.execution.base.ZigExecConfigBase;
import com.falsepattern.zigbrains.project.util.CLIUtil;
import com.intellij.execution.ExecutionException;
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

import static com.falsepattern.zigbrains.project.execution.base.ZigConfigEditor.*;

@Getter
public class ZigExecConfigBuild extends ZigExecConfigBase<ZigExecConfigBuild> {
    private ArgsConfigurable buildSteps = new ArgsConfigurable("buildSteps", "Build steps");
    private ArgsConfigurable extraArgs = new ArgsConfigurable("extraArgs", "Extra command line arguments");
    private CheckboxConfigurable colored = coloredConfigurable("colored");
    private FilePathConfigurable exePath = new FilePathConfigurable("exePath", "Output executable created by the build (debugging, autodetect if empty)");
    private ArgsConfigurable exeArgs = new ArgsConfigurable("exeArgs", "Command line arguments for executable (debugging)");
    public ZigExecConfigBuild(@NotNull Project project, @NotNull ConfigurationFactory factory) {
        super(project, factory, "Zig Build");
    }

    @Override
    public List<String> buildCommandLineArgs(boolean debug) throws ExecutionException {
        val result = new ArrayList<String>();
        result.add("build");
        var steps = List.of(buildSteps.args);
        if (debug) {
            val truncatedSteps = new ArrayList<String>();
            for (int i = 0, size = steps.size(); i < size; i++) {
                if (steps.get(i).equals("run")) {
                    continue;
                }
                if (steps.get(i).equals("test")) {
                    throw new ExecutionException("Debugging \"zig build test\" is not supported yet.");
                }
                truncatedSteps.add(steps.get(i));
            }
            steps = truncatedSteps;
        }
        result.addAll(steps);
        result.addAll(CLIUtil.colored(colored.value, debug));
        result.addAll(List.of(extraArgs.args));
        return result;
    }

    @Override
    public @Nullable String suggestedName() {
        return "Build";
    }

    @Override
    public @NotNull List<ZigConfigurable<?>> getConfigurables() {
        val baseCfg = CollectionUtil.concat(super.getConfigurables(), buildSteps, extraArgs, colored);
        if (ZBFeatures.debug()) {
            return CollectionUtil.concat(baseCfg, exePath, exeArgs);
        } else {
            return baseCfg;
        }
    }

    @Override
    public ZigExecConfigBuild clone() {
        val clone = super.clone();
        clone.buildSteps = buildSteps.clone();
        clone.extraArgs = extraArgs.clone();
        clone.colored = colored.clone();
        clone.exePath = exePath.clone();
        clone.exeArgs = exeArgs.clone();
        return clone;
    }

    @Override
    public @Nullable ProfileStateBuild getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
        return new ProfileStateBuild(environment, this);
    }
}
