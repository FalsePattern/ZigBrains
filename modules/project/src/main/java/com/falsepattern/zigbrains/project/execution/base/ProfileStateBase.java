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

import com.falsepattern.zigbrains.project.runconfig.ZigProcessHandler;
import com.falsepattern.zigbrains.project.toolchain.AbstractZigToolchain;
import com.falsepattern.zigbrains.project.util.ProjectUtil;
import com.intellij.build.BuildTextConsoleView;
import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PtyCommandLine;
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
        val toolchain = ProjectUtil.getToolchain(getEnvironment().getProject());
        if (toolchain == null) {
            throw new ExecutionException("Failed to get zig toolchain from project");
        }
        return new ZigProcessHandler(getCommandLine(toolchain, false));
    }

    public GeneralCommandLine getCommandLine(AbstractZigToolchain toolchain, boolean debug) throws ExecutionException {
        val workingDirectory = configuration.getWorkingDirectory();
        val zigExecutablePath = toolchain.pathToExecutable("zig");

        // TODO remove this check once JetBrains implements colored terminal in the debugger
        // https://youtrack.jetbrains.com/issue/CPP-11622/ANSI-color-codes-not-honored-in-Debug-Run-Configuration-output-window
        val cli = debug ? new GeneralCommandLine() : new PtyCommandLine();
        cli.setExePath(zigExecutablePath.toString());
        workingDirectory.getPath().ifPresent(x -> cli.setWorkDirectory(x.toFile()));
        cli.setCharset(StandardCharsets.UTF_8);
        cli.setRedirectErrorStream(true);
        cli.addParameters(configuration.buildCommandLineArgs(debug));
        return cli;
    }

    public T configuration() {
        return configuration;
    }

    public DefaultExecutionResult executeCommandLine(GeneralCommandLine commandLine, ExecutionEnvironment environment)
            throws ExecutionException {
        val handler = startProcess(commandLine);
        val console = new BuildTextConsoleView(environment.getProject(), false, Collections.emptyList());
        console.attachToProcess(handler);
        return new DefaultExecutionResult(console, handler);
    }

    public static ProcessHandler startProcess(GeneralCommandLine commandLine) throws ExecutionException {
        val handler = new ZigProcessHandler(commandLine);
        ProcessTerminatedListener.attach(handler);
        return handler;
    }
}
