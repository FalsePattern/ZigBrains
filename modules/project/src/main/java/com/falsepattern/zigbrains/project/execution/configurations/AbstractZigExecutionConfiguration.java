/*
 * Copyright 2023 FalsePattern
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

package com.falsepattern.zigbrains.project.execution.configurations;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.LocatableConfigurationBase;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsActions;
import com.intellij.openapi.util.text.Strings;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Optional;

public abstract class AbstractZigExecutionConfiguration extends LocatableConfigurationBase<RunProfileState> {
    public @Nullable Path workingDirectory;
    public AbstractZigExecutionConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory, @Nullable String name) {
        super(project, factory, name);
        workingDirectory = project.isDefault() ? null : Optional.ofNullable(project.getBasePath())
                                                                .map(Path::of)
                                                                .orElse(null);
    }

    public abstract String getCommand();
    public abstract void setCommand(String value);

    @Override
    public @Nullable @NlsActions.ActionText String suggestedName() {
        val cmd = getCommand();
        var spaceIndex = cmd.indexOf(' ');
        return Strings.capitalize(spaceIndex > 0 ? cmd.substring(0, spaceIndex) : cmd);
    }

    @Override
    public abstract @Nullable RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment)
            throws ExecutionException;
}
