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
public class ZigExecConfigBinary extends ZigExecConfigBase<ZigExecConfigBinary> {
    private ZigConfigEditor.FilePathConfigurable exePath = new ZigConfigEditor.FilePathConfigurable("exePath", "Executable program path (not the zig compiler)");
    private ZigConfigEditor.ArgsConfigurable args = new ZigConfigEditor.ArgsConfigurable("args", "Command line arguments");

    public ZigExecConfigBinary(@NotNull Project project, @NotNull ConfigurationFactory factory) {
        super(project, factory, "Zig-compiled native executable");
    }

    @Override
    public String[] buildCommandLineArgs() {
        return args.args;
    }

    @Override
    public @Nullable String suggestedName() {
        return "Executable";
    }


    @Override
    public ZigExecConfigBinary clone() {
        val clone = super.clone();
        clone.exePath = exePath.clone();
        clone.args = args.clone();
        return clone;
    }

    @Override
    public @NotNull List<ZigConfigEditor.ZigConfigurable<?>> getConfigurables() {
        return CollectionUtil.concat(super.getConfigurables(), exePath, args);
    }

    @Override
    public @Nullable ProfileStateBinary getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
        return new ProfileStateBinary(environment, this);
    }
}
