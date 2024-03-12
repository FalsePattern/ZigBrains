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

package com.falsepattern.zigbrains.project.runconfig;

import com.falsepattern.zigbrains.project.execution.base.ProfileStateBase;
import com.falsepattern.zigbrains.project.execution.base.ZigExecConfigBase;
import com.falsepattern.zigbrains.project.toolchain.AbstractZigToolchain;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.DefaultProgramRunnerKt;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ZigRegularRunner extends ZigProgramRunnerBase<ProfileStateBase<?>> {
    public ZigRegularRunner() {
        super(DefaultRunExecutor.EXECUTOR_ID);
    }

    @Override
    public @NotNull @NonNls String getRunnerId() {
        return "ZigRegularRunner";
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return this.executorId.equals(executorId) && profile instanceof ZigExecConfigBase<?>;
    }

    @Override
    protected @Nullable ProfileStateBase<?> castProfileState(ProfileStateBase<?> state) {
        return state;
    }

    @Override
    protected @Nullable RunContentDescriptor doExecute(ProfileStateBase<?> state, AbstractZigToolchain toolchain, ExecutionEnvironment environment)
            throws ExecutionException {
        return DefaultProgramRunnerKt.showRunContent(state.executeCommandLine(state.getCommandLine(toolchain, false), environment), environment);
    }
}
