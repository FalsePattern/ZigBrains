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

package com.falsepattern.zigbrains.debugger.runner.run;

import com.falsepattern.zigbrains.debugger.runner.base.ZigDebugParametersBase;
import com.falsepattern.zigbrains.debugger.runner.base.ZigDebugRunnerBase;
import com.falsepattern.zigbrains.project.execution.base.ProfileStateBase;
import com.falsepattern.zigbrains.project.execution.run.ProfileStateRun;
import com.falsepattern.zigbrains.project.execution.run.ZigExecConfigRun;
import com.falsepattern.zigbrains.project.toolchain.AbstractZigToolchain;
import com.falsepattern.zigbrains.project.toolchain.LocalZigToolchain;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ZigDebugRunnerRun extends ZigDebugRunnerBase<ProfileStateRun> {
    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return this.executorId.equals(executorId) &&
               (profile instanceof ZigExecConfigRun);
    }

    @Override
    public @NotNull @NonNls String getRunnerId() {
        return "ZigDebugRunnerRun";
    }

    @Override
    protected ZigDebugParametersRun getDebugParameters(ProfileStateRun profileStateRun, ExecutionEnvironment environment, DebuggerDriverConfiguration debuggerDriver, AbstractZigToolchain toolchain) throws
            ExecutionException {
        return new ZigDebugParametersRun(debuggerDriver, LocalZigToolchain.ensureLocal(toolchain), profileStateRun);
    }

    @Override
    protected @Nullable ProfileStateRun castProfileState(ProfileStateBase<?> state) {
        return state instanceof ProfileStateRun state$ ? state$ : null;
    }
}
