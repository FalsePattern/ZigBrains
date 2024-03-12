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

package com.falsepattern.zigbrains.debugger.runner.binary;

import com.falsepattern.zigbrains.debugger.runner.base.ZigDebugParametersBase;
import com.falsepattern.zigbrains.debugger.runner.base.ZigDebugRunnerBase;
import com.falsepattern.zigbrains.project.execution.base.ProfileStateBase;
import com.falsepattern.zigbrains.project.execution.binary.ProfileStateBinary;
import com.falsepattern.zigbrains.project.execution.binary.ZigExecConfigBinary;
import com.falsepattern.zigbrains.project.toolchain.AbstractZigToolchain;
import com.falsepattern.zigbrains.project.toolchain.LocalZigToolchain;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ZigDebugRunnerBinary extends ZigDebugRunnerBase<ProfileStateBinary> {
    @Override
    public @NotNull @NonNls String getRunnerId() {
        return "ZigDebugRunnerBinary";
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return this.executorId.equals(executorId) &&
               (profile instanceof ZigExecConfigBinary);
    }

    @Override
    protected @Nullable ZigDebugParametersBase<ProfileStateBinary> getDebugParameters(ProfileStateBinary profileStateBinary, ExecutionEnvironment environment, DebuggerDriverConfiguration debuggerDriver, AbstractZigToolchain toolchain$) {
        if (!(toolchain$ instanceof LocalZigToolchain toolchain)) {
            Notifications.Bus.notify(new Notification("ZigBrains.Debugger.Error", "The debugger only supports local zig toolchains!", NotificationType.ERROR));
            return null;
        }
        return new ZigDebugParametersBinary(debuggerDriver, toolchain, profileStateBinary);
    }

    @Override
    protected @Nullable ProfileStateBinary castProfileState(ProfileStateBase<?> state) {
        return state instanceof ProfileStateBinary state$ ? state$ : null;
    }
}
