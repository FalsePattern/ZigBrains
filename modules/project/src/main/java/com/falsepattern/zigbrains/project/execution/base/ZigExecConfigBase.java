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

import com.falsepattern.zigbrains.project.ui.WorkingDirectoryComponent;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.LocatableConfigurationBase;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.NlsActions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

public abstract class ZigExecConfigBase<T extends ZigExecConfigBase<T>> extends LocatableConfigurationBase<ProfileStateBase<T>> {
    public @Nullable Path workingDirectory;
    public ZigExecConfigBase(@NotNull Project project, @NotNull ConfigurationFactory factory, @Nullable String name) {
        super(project, factory, name);
        workingDirectory = project.isDefault() ? null : Optional.ofNullable(project.getBasePath())
                                                                .map(Path::of)
                                                                .orElse(null);
    }

    public abstract String[] buildCommandLineArgs();

    @Override
    public abstract @Nullable @NlsActions.ActionText String suggestedName();

    @Override
    public abstract @Nullable ProfileStateBase<T> getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment)
            throws ExecutionException;

    public abstract static class EditorBase<T extends ZigExecConfigBase<T>> extends SettingsEditor<T> {
        protected final LabeledComponent<TextFieldWithBrowseButton> workingDirectoryComponent =
                new WorkingDirectoryComponent();

        @Override
        protected void applyEditorTo(@NotNull T s) throws ConfigurationException {
            s.workingDirectory = Paths.get(workingDirectoryComponent.getComponent().getText());
        }

        @Override
        protected void resetEditorFrom(@NotNull T s) {
            workingDirectoryComponent.getComponent()
                                     .setText(Objects.requireNonNullElse(s.workingDirectory, "")
                                                     .toString());
        }
    }
}
