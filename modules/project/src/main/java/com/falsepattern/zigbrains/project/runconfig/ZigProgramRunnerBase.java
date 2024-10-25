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
import com.falsepattern.zigbrains.project.openapi.components.ZigProjectSettingsService;
import com.falsepattern.zigbrains.project.toolchain.AbstractZigToolchain;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.runners.AsyncProgramRunner;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;

public abstract class ZigProgramRunnerBase<ProfileState extends ProfileStateBase<?>> extends
        AsyncProgramRunner<RunnerSettings> {
    protected final String executorId;

    public ZigProgramRunnerBase(String executorId) {
        this.executorId = executorId;
    }

    @Override
    protected Promise<RunContentDescriptor> execute(@NotNull ExecutionEnvironment environment, @NotNull RunProfileState state$) {
        if (!(state$ instanceof ProfileStateBase<?> state$$)) {
            return Promises.resolvedPromise();
        }
        val state = castProfileState(state$$);
        if (state == null)
            return Promises.resolvedPromise();

        val toolchain = ZigProjectSettingsService.getInstance(environment.getProject()).getState().getToolchain();
        if (toolchain == null) {
            return Promises.resolvedPromise();
        }

        FileDocumentManager.getInstance().saveAllDocuments();

        val runContentDescriptorPromise = new AsyncPromise<RunContentDescriptor>();
        AppExecutorUtil.getAppExecutorService().execute(() -> {
            try {
                doExecuteAsync(state, toolchain, environment, runContentDescriptorPromise);
            } catch (ExecutionException e) {
                runContentDescriptorPromise.setError(e);
            }
        });
        return runContentDescriptorPromise;
    }

    protected abstract @Nullable ProfileState castProfileState(ProfileStateBase<?> state);

    protected abstract void doExecuteAsync(ProfileState state,
                                           AbstractZigToolchain toolchain,
                                           ExecutionEnvironment environment,
                                           AsyncPromise<RunContentDescriptor> runContentDescriptorPromise) throws ExecutionException;
}
