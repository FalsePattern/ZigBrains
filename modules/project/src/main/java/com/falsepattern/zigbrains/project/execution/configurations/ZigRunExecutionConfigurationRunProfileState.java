/*
 * Copyright 2023 FalsePattern
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

package com.falsepattern.zigbrains.project.execution.configurations;

import com.falsepattern.zigbrains.project.execution.ZigCapturingProcessHandler;
import com.falsepattern.zigbrains.project.util.ProjectUtil;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public record ZigRunExecutionConfigurationRunProfileState(ExecutionEnvironment environment, ZigRunExecutionConfiguration configuration)
        implements RunProfileState {
    @Override
    public @NotNull ExecutionResult execute(Executor executor, @NotNull ProgramRunner<?> runner)
            throws ExecutionException {
        val state = new CommandLineState(environment) {

            @Override
            protected @NotNull ProcessHandler startProcess() throws ExecutionException {
                val workingDirectory = configuration.workingDirectory;
                val zigExecutablePath = Objects.requireNonNull(ProjectUtil.getToolchain(environment.getProject()))
                                               .pathToExecutable("zig");

                val commandLine = new GeneralCommandLine()
                        .withExePath(zigExecutablePath.toString())
                        .withWorkDirectory(workingDirectory.toString())
                        .withCharset(StandardCharsets.UTF_8)
                        .withRedirectErrorStream(true)
                        .withParameters(configuration.command.split(" "));

                return new ZigCapturingProcessHandler(commandLine);
            }
        };

        return state.execute(executor, runner);
    }
}
