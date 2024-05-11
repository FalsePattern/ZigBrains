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

package com.falsepattern.zigbrains.debugger.runner.base;

import com.falsepattern.zigbrains.project.execution.base.ProfileStateBase;
import com.falsepattern.zigbrains.project.runconfig.ZigProgramRunnerBase;
import com.falsepattern.zigbrains.project.toolchain.AbstractZigToolchain;
import com.falsepattern.zigbrains.debugger.Utils;
import com.falsepattern.zigbrains.debugger.ZigLocalDebugProcess;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.jetbrains.cidr.execution.Installer;
import com.jetbrains.cidr.execution.TrivialRunParameters;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public abstract class ZigDebugRunnerBase<ProfileState extends ProfileStateBase<?>> extends ZigProgramRunnerBase<ProfileState> {
    public ZigDebugRunnerBase() {
        super(DefaultDebugExecutor.EXECUTOR_ID);
    }

    private static RunContentDescriptor startSession(ExecutionEnvironment environment, XDebugProcessStarter starter)
            throws ExecutionException {
        return XDebuggerManager.getInstance(environment.getProject())
                               .startSession(environment, starter)
                               .getRunContentDescriptor();
    }

    @Override
    protected RunContentDescriptor doExecute(ProfileState state, AbstractZigToolchain toolchain, ExecutionEnvironment environment)
            throws ExecutionException {
        val project = environment.getProject();
        val debuggerDriver = Utils.getDebuggerConfiguration(project);
        if (debuggerDriver == null) {
            Notifications.Bus.notify(new Notification("ZigBrains.Debugger.Error", "Couldn't find a working GDB or LLDB debugger! Please check your Toolchains! (Settings | Build, Execution, Deployment | Toolchains)", NotificationType.ERROR));
            return null;
        }
        Either<ZigDebugParametersBase<ProfileState>, ExecutionException> runParameters;

        try {
            runParameters = ApplicationManager.getApplication().executeOnPooledThread(() -> getDebugParametersSafe(state, environment, debuggerDriver, toolchain)).get();
        } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
            e.printStackTrace();
            Notifications.Bus.notify(new Notification("ZigBrains.Debugger.Error", e.getMessage(), NotificationType.ERROR));
            return null;
        }
        if (runParameters == null) {
            //Assume that getDebugParameters reports the bug in a notification already
            return null;
        }

        if (runParameters.isRight()) {
            return startSession(environment, new ErrorProcessStarter(state, runParameters.getRight(), debuggerDriver));
        } else if (runParameters.isLeft()) {
            return startSession(environment, new ZigLocalDebugProcessStarter(runParameters.getLeft(), state, environment));
        } else {
            return null;
        }
    }

    private Either<ZigDebugParametersBase<ProfileState>, ExecutionException> getDebugParametersSafe(ProfileState state, ExecutionEnvironment environment, DebuggerDriverConfiguration debuggerDriver, AbstractZigToolchain toolchain) {
        try {
            return Either.forLeft(getDebugParameters(state, environment, debuggerDriver, toolchain));
        } catch (ExecutionException e) {
            return Either.forRight(e);
        }
    }

    @Override
    public abstract boolean canRun(@NotNull String executorId, @NotNull RunProfile profile);

    protected abstract @Nullable ZigDebugParametersBase<ProfileState> getDebugParameters(ProfileState state, ExecutionEnvironment environment, DebuggerDriverConfiguration debuggerDriver, AbstractZigToolchain toolchain) throws ExecutionException;

    @RequiredArgsConstructor
    private class ZigLocalDebugProcessStarter extends XDebugProcessStarter {
        private final ZigDebugParametersBase<ProfileState> params;
        private final ProfileState state;
        private final ExecutionEnvironment environment;

        @Override
        public @NotNull XDebugProcess start(@NotNull XDebugSession session) throws ExecutionException {
            val process = new ZigLocalDebugProcess(params, session, state.getConsoleBuilder());
            ProcessTerminatedListener.attach(process.getProcessHandler(), environment.getProject());
            process.start();
            return process;
        }
    }

    @RequiredArgsConstructor
    private class ErrorProcessStarter extends XDebugProcessStarter {
        private final ProfileState state;
        private final ExecutionException exception;
        private final DebuggerDriverConfiguration debuggerDriver;

        @Override
        public @NotNull XDebugProcess start(@NotNull XDebugSession session) throws ExecutionException {
            val cb = state.getConsoleBuilder();
            val wrappedBuilder = new TextConsoleBuilder() {
                @Override
                public @NotNull ConsoleView getConsole() {
                    val console = cb.getConsole();
                    console.print(exception.getMessage(), ConsoleViewContentType.ERROR_OUTPUT);
                    return console;
                }

                @Override
                public void addFilter(@NotNull Filter filter) {
                    cb.addFilter(filter);
                }

                @Override
                public void setViewer(boolean isViewer) {
                    cb.setViewer(isViewer);
                }
            };
            val process = new ZigLocalDebugProcess(new TrivialRunParameters(debuggerDriver, new Installer() {
                @Override
                public @NotNull GeneralCommandLine install() throws ExecutionException {
                    throw new ExecutionException("Failed to start debugging");
                }

                @Override
                public @NotNull File getExecutableFile() {
                    return null;
                }
            }), session, wrappedBuilder);
            process.start();
            return process;
        }
    }
}
