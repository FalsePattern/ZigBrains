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

import com.falsepattern.zigbrains.common.util.ApplicationUtil;
import com.falsepattern.zigbrains.debugbridge.ZigDebuggerDriverConfigurationProvider;
import com.falsepattern.zigbrains.debugger.Utils;
import com.falsepattern.zigbrains.debugger.ZigLocalDebugProcess;
import com.falsepattern.zigbrains.project.execution.base.ProfileStateBase;
import com.falsepattern.zigbrains.project.runconfig.ZigProgramRunnerBase;
import com.falsepattern.zigbrains.project.toolchain.AbstractZigToolchain;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        val drivers = ZigDebuggerDriverConfigurationProvider.findDebuggerConfigurations(project, false, false)
                                                              .toList();

        for (val debuggerDriver: drivers) {
            if (debuggerDriver == null)
                continue;
            ZigDebugParametersBase<ProfileState> runParameters = getDebugParameters(state, environment, debuggerDriver, toolchain);
            if (runParameters == null) {
                continue;
            }
            return startSession(environment, new ZigLocalDebugProcessStarter(runParameters, state, environment));
        }
        return null;
    }

    @Override
    public abstract boolean canRun(@NotNull String executorId, @NotNull RunProfile profile);

    protected abstract @Nullable ZigDebugParametersBase<ProfileState> getDebugParameters(ProfileState state, ExecutionEnvironment environment, DebuggerDriverConfiguration debuggerDriver, AbstractZigToolchain toolchain) throws ExecutionException;

    @RequiredArgsConstructor
    private class ZigLocalDebugProcessStarter extends XDebugProcessStarter {
        private final ZigDebugParametersBase<ProfileState> params;
        private final ProfileState state;
        private final ExecutionEnvironment environment;
        private static class Carrier {
            volatile ConsoleView console;
            final Map<ConsoleViewContentType, List<String>> outputs = new HashMap<>();

            void handleOutput(String text, ConsoleViewContentType type) {
                if (console != null) {
                    console.print(text, type);
                } else {
                    outputs.computeIfAbsent(type, (ignored) -> new ArrayList<>()).add(text);
                }
            }
        }

        @Override
        public @NotNull XDebugProcess start(@NotNull XDebugSession session) throws ExecutionException {
            val cb = state.getConsoleBuilder();
            val carrier = new Carrier();
            val wrappedBuilder = new TextConsoleBuilder() {
                @Override
                public @NotNull ConsoleView getConsole() {
                    val console = cb.getConsole();
                    for (val output: carrier.outputs.entrySet()) {
                        for (val line: output.getValue()) {
                            console.print(line + "\n", output.getKey());
                        }
                    }
                    carrier.console = console;
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
            val process = new ZigLocalDebugProcess(params, session, wrappedBuilder);
            if (params instanceof PreLaunchAware pla) {
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    ProcessTerminatedListener.attach(process.getProcessHandler(), environment.getProject());
                    try {
                        pla.preLaunch();
                    } catch (Exception e) {
                        ApplicationUtil.invokeLater(() -> {
                            if (e instanceof Utils.ProcessException pe) {
                                carrier.handleOutput(pe.command + "\n", ConsoleViewContentType.SYSTEM_OUTPUT);
                                carrier.handleOutput("Compilation failure!\n", ConsoleViewContentType.SYSTEM_OUTPUT);
                                carrier.handleOutput(pe.stdout, ConsoleViewContentType.NORMAL_OUTPUT);
                                carrier.handleOutput(pe.stderr, ConsoleViewContentType.ERROR_OUTPUT);
                                process.handleTargetTerminated(new DebuggerDriver.ExitStatus(pe.exitCode));
                            } else {
                                carrier.handleOutput("Exception while compiling binary:\n", ConsoleViewContentType.SYSTEM_OUTPUT);
                                carrier.handleOutput(e.getMessage(), ConsoleViewContentType.ERROR_OUTPUT);
                                process.handleTargetTerminated(new DebuggerDriver.ExitStatus(-1));
                            }
                            process.stop();
                            ApplicationManager.getApplication().executeOnPooledThread(() -> process.unSuppress(false));
                        });
                        return;
                    }
                    process.unSuppress(true);
                });
            }
            process.doStart();
            return process;
        }
    }
}
