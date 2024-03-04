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

package com.falsepattern.zigbrains.project.execution.base;

import com.falsepattern.zigbrains.project.execution.ZigCapturingProcessHandler;
import com.falsepattern.zigbrains.project.runconfig.ZigProcessHandler;
import com.falsepattern.zigbrains.project.toolchain.AbstractZigToolchain;
import com.falsepattern.zigbrains.project.util.ProjectUtil;
import com.intellij.build.BuildTextConsoleView;
import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

public abstract class ProfileStateBase<T extends ZigExecConfigBase<T>> extends CommandLineState {
    protected final T configuration;

    public ProfileStateBase(ExecutionEnvironment environment, T configuration) {
        super(environment);
        this.configuration = configuration;
    }

    @Override
    protected @NotNull ProcessHandler startProcess() throws ExecutionException {
        return new ZigProcessHandler(getCommandLine(ProjectUtil.getToolchain(getEnvironment().getProject())));
    }

    public GeneralCommandLine getCommandLine(AbstractZigToolchain toolchain) {
        val workingDirectory = configuration.workingDirectory;
        val zigExecutablePath = toolchain.pathToExecutable("zig");

        return new GeneralCommandLine().withExePath(zigExecutablePath.toString())
                                       .withWorkDirectory(workingDirectory.toString())
                                       .withCharset(StandardCharsets.UTF_8)
                                       .withRedirectErrorStream(true)
                                       .withParameters(configuration.buildCommandLineArgs());
    }

    public T configuration() {
        return configuration;
    }

    public DefaultExecutionResult executeCommandLine(GeneralCommandLine commandLine, ExecutionEnvironment environment)
            throws ExecutionException {
        val handler = startProcess(commandLine);
        val console = new BuildTextConsoleView(environment.getProject(), true, Collections.emptyList());
        console.attachToProcess(handler);
        return new DefaultExecutionResult(console, handler);
    }

    public static ProcessHandler startProcess(GeneralCommandLine commandLine) throws ExecutionException {
        val handler = new ZigProcessHandler(commandLine);
        ProcessTerminatedListener.attach(handler);
        return handler;
    }
}
