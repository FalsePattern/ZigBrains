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

import com.falsepattern.zigbrains.debugbridge.ZigDebuggerDriverConfigurationProvider;
import com.falsepattern.zigbrains.debugger.ZigLocalDebugProcess;
import com.falsepattern.zigbrains.project.execution.base.ProfileStateBase;
import com.falsepattern.zigbrains.project.runconfig.ZigProgramRunnerBase;
import com.falsepattern.zigbrains.project.toolchain.AbstractZigToolchain;
import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.RunContentBuilder;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.AsyncPromise;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class ZigDebugRunnerBase<ProfileState extends ProfileStateBase<?>> extends ZigProgramRunnerBase<ProfileState> {
    public ZigDebugRunnerBase() {
        super(DefaultDebugExecutor.EXECUTOR_ID);
    }

    private boolean doExecuteAsyncWithDriver(ProfileState state,
                                             AbstractZigToolchain toolchain,
                                             ExecutionEnvironment environment,
                                             AsyncPromise<@Nullable RunContentDescriptor> runContentDescriptorPromise,
                                             DebuggerDriverConfiguration debuggerDriver) throws ExecutionException {
        ZigDebugParametersBase<ProfileState> runParameters = getDebugParameters(state, environment, debuggerDriver, toolchain);
        if (runParameters == null) {
            return false;
        }
        val console = state.getConsoleBuilder().getConsole();
        if (runParameters instanceof PreLaunchAware pla) {
            val listener = new PreLaunchProcessListener(console);
            pla.preLaunch(listener);
            if (listener.isBuildFailed()) {
                val executionResult = new DefaultExecutionResult(console, listener.getProcessHandler());
                ApplicationManager.getApplication().invokeLater(() -> {
                    val runContentBuilder = new RunContentBuilder(executionResult, environment);
                    val runContentDescriptor = runContentBuilder.showRunContent(null);
                    runContentDescriptorPromise.setResult(runContentDescriptor);
                });
                return true;
            }
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            val debuggerManager = XDebuggerManager.getInstance(environment.getProject());
            try {
                val xDebugSession = debuggerManager.startSession(environment, new XDebugProcessStarter() {
                    @Override
                    public @NotNull XDebugProcess start(@NotNull XDebugSession session)
                            throws ExecutionException {
                        val project = session.getProject();
                        val textConsoleBuilder = new SharedConsoleBuilder(console);
                        val debugProcess = new ZigLocalDebugProcess(runParameters, session, textConsoleBuilder);
                        ProcessTerminatedListener.attach(debugProcess.getProcessHandler(), project);
                        debugProcess.start();
                        return debugProcess;
                    }
                });
                runContentDescriptorPromise.setResult(xDebugSession.getRunContentDescriptor());
            } catch (ExecutionException e) {
                runContentDescriptorPromise.setError(e);
            }
        });
        return true;
    }

    private void doExecuteAsyncFetchNextDriver(ProfileState state,
                                               AbstractZigToolchain toolchain,
                                               ExecutionEnvironment environment,
                                               AsyncPromise<@Nullable RunContentDescriptor> runContentDescriptorPromise,
                                               List<Supplier<DebuggerDriverConfiguration>> drivers) {
        if (drivers.isEmpty()) {
            runContentDescriptorPromise.setResult(null);
            return;
        }

        val driverSupplier = drivers.remove(0);
        val driver = driverSupplier.get();
        AppExecutorUtil.getAppExecutorService().execute(() -> {
            try {
                if (!doExecuteAsyncWithDriver(state, toolchain, environment, runContentDescriptorPromise, driver)) {
                    ApplicationManager.getApplication().invokeLater(() -> doExecuteAsyncFetchNextDriver(state, toolchain, environment, runContentDescriptorPromise, drivers));
                }
            } catch (ExecutionException e) {
                runContentDescriptorPromise.setError(e);
            }
        });
    }

    @Override
    protected void doExecuteAsync(ProfileState state,
                                  AbstractZigToolchain toolchain,
                                  ExecutionEnvironment environment,
                                  AsyncPromise<@Nullable RunContentDescriptor> runContentDescriptorPromise) {
        val project = environment.getProject();
        val drivers = ZigDebuggerDriverConfigurationProvider.findDebuggerConfigurations(project, false, false)
                                                            .collect(Collectors.toCollection(ArrayList::new));

        ApplicationManager.getApplication()
                          .invokeLater(() -> doExecuteAsyncFetchNextDriver(state, toolchain, environment, runContentDescriptorPromise, drivers));
    }

    @Override
    public abstract boolean canRun(@NotNull String executorId, @NotNull RunProfile profile);

    protected abstract @Nullable ZigDebugParametersBase<ProfileState> getDebugParameters(ProfileState state, ExecutionEnvironment environment, DebuggerDriverConfiguration debuggerDriver, AbstractZigToolchain toolchain) throws ExecutionException;

    @RequiredArgsConstructor
    private static class SharedConsoleBuilder extends TextConsoleBuilder {
        private final ConsoleView console;
        @Override
        public @NotNull ConsoleView getConsole() {
            return console;
        }

        @Override
        public void addFilter(@NotNull Filter filter) {

        }

        @Override
        public void setViewer(boolean b) {

        }
    }
}
