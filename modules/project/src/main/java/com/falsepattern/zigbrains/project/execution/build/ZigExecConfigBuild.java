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
public class ZigExecConfigBuild extends ZigExecConfigBase<ZigExecConfigBuild> {
    private ZigConfigEditor.ArgsConfigurable extraArgs = new ZigConfigEditor.ArgsConfigurable("extraArgs", "Extra command line arguments");
    private ZigConfigEditor.ColoredConfigurable colored = new ZigConfigEditor.ColoredConfigurable("colored");
    private ZigConfigEditor.FilePathConfigurable exePath = new ZigConfigEditor.FilePathConfigurable("exePath", "Output executable created by the build (for debugging)");
    public ZigExecConfigBuild(@NotNull Project project, @NotNull ConfigurationFactory factory) {
        super(project, factory, "Zig Build");
    }

    @Override
    public String[] buildCommandLineArgs() {
        val base = new String[]{"build", "--color", colored.colored ? "on" : "off"};
        return CollectionUtil.concat(base, extraArgs.args);
    }

    @Override
    public @Nullable String suggestedName() {
        return "Build";
    }

    @Override
    public @NotNull List<ZigConfigEditor.ZigConfigurable<?>> getConfigurables() {
        return CollectionUtil.concat(super.getConfigurables(), extraArgs, colored, exePath);
    }

    @Override
    public ZigExecConfigBuild clone() {
        val clone = super.clone();
        clone.extraArgs = extraArgs.clone();
        clone.colored = colored.clone();
        clone.exePath = exePath.clone();
        return clone;
    }

    @Override
    public @Nullable ProfileStateBuild getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
        return new ProfileStateBuild(environment, this);
    }
}
