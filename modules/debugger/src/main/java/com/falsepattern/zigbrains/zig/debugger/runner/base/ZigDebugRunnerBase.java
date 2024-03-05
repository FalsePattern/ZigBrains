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

package com.falsepattern.zigbrains.zig.debugger.runner.base;

import com.falsepattern.zigbrains.project.execution.base.ProfileStateBase;
import com.falsepattern.zigbrains.project.runconfig.ZigProgramRunnerBase;
import com.falsepattern.zigbrains.project.toolchain.AbstractZigToolchain;
import com.falsepattern.zigbrains.zig.debugger.Utils;
import com.falsepattern.zigbrains.zig.debugger.ZigLocalDebugProcess;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ZigDebugRunnerBase<ProfileState extends ProfileStateBase<?>> extends ZigProgramRunnerBase<ProfileState> {
    public ZigDebugRunnerBase() {
        super(DefaultDebugExecutor.EXECUTOR_ID);
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
        val runParameters = getDebugParameters(state, environment, debuggerDriver, toolchain);
        if (runParameters == null) {
            //Assume that getDebugParameters reports the bug in a notification already
            return null;
        }
        val manager = XDebuggerManager.getInstance(project);
        return manager.startSession(environment,
                                    new XDebugProcessStarter() {
            @Override
            public @NotNull XDebugProcess start(@NotNull XDebugSession session) throws ExecutionException {
                val process = new ZigLocalDebugProcess(runParameters, session, state.getConsoleBuilder());
                ProcessTerminatedListener.attach(process.getProcessHandler(), environment.getProject());
                process.start();
                return process;
            }
        }).getRunContentDescriptor();
    }

    @Override
    public abstract boolean canRun(@NotNull String executorId, @NotNull RunProfile profile);

    protected abstract @Nullable ZigDebugParametersBase<ProfileState> getDebugParameters(ProfileState state, ExecutionEnvironment environment, DebuggerDriverConfiguration debuggerDriver, AbstractZigToolchain toolchain);
}
