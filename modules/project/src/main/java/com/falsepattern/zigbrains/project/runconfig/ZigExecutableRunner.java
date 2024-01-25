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

import com.falsepattern.zigbrains.project.execution.configurations.AbstractZigExecutionConfiguration;
import com.falsepattern.zigbrains.project.execution.configurations.ZigRunExecutionConfigurationRunProfileState;
import com.falsepattern.zigbrains.project.openapi.components.ZigProjectSettingsService;
import com.falsepattern.zigbrains.zig.lsp.ZLSEditorEventManager;
import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.DefaultProgramRunnerKt;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ZigExecutableRunner extends ZigDefaultProgramRunnerBase {
    protected final String executorId;
    private final String errorMessageTitle;

    public ZigExecutableRunner(String executorId, String errorMessageTitle) {
        this.executorId = executorId;
        this.errorMessageTitle = errorMessageTitle;
    }


    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return executorId.equals(this.executorId) && profile instanceof AbstractZigExecutionConfiguration;
    }


    @Override
    protected void execute(@NotNull ExecutionEnvironment environment, @NotNull RunProfileState state) {
        super.execute(environment, state);
    }

    @Override
    protected @Nullable RunContentDescriptor doExecute(@NotNull RunProfileState state$, @NotNull ExecutionEnvironment environment)
            throws ExecutionException {
        if (!(state$ instanceof ZigRunExecutionConfigurationRunProfileState state)) {
            return null;
        }

        val toolchain = ZigProjectSettingsService.getInstance(environment.getProject()).getToolchain();
        if (toolchain == null) {
            return null;
        }

        FileDocumentManager.getInstance().saveAllDocuments();

        val cli = state.getCommandLine(toolchain);

        return showRunContent(state, environment, cli);
    }

    protected RunContentDescriptor showRunContent(ZigRunExecutionConfigurationRunProfileState state,
                                                  ExecutionEnvironment environment,
                                                  GeneralCommandLine runExecutable) throws ExecutionException {
        return DefaultProgramRunnerKt.showRunContent(executeCommandLine(state, runExecutable, environment), environment);
    }

    private DefaultExecutionResult executeCommandLine(ZigRunExecutionConfigurationRunProfileState state,
                                                      GeneralCommandLine commandLine,
                                                      ExecutionEnvironment environment) throws ExecutionException {
        return state.executeCommandLine(commandLine, environment);
    }
}
