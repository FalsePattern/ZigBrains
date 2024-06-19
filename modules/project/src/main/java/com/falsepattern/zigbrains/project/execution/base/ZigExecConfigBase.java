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

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.LocatableConfigurationBase;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.NlsActions;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.val;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

@Getter
public abstract class ZigExecConfigBase<T extends ZigExecConfigBase<T>> extends LocatableConfigurationBase<ProfileStateBase<T>> {
    private ZigConfigEditor.WorkDirectoryConfigurable workingDirectory = new ZigConfigEditor.WorkDirectoryConfigurable("workingDirectory");
    private ZigConfigEditor.CheckboxConfigurable pty = new ZigConfigEditor.CheckboxConfigurable("pty", "Emulate Terminal", false);
    public ZigExecConfigBase(@NotNull Project project, @NotNull ConfigurationFactory factory, @Nullable String name) {
        super(project, factory, name);
        workingDirectory.setPath(getProject().isDefault() ? null : Optional.ofNullable(ProjectUtil.guessProjectDir(getProject()))
                                                                           .map(VirtualFile::toNioPath)
                                                                           .orElse(null));
    }

    @Override
    public @NotNull ZigConfigEditor<T> getConfigurationEditor() {
        return new ZigConfigEditor<>(this);
    }

    @Override
    public void readExternal(@NotNull Element element) throws InvalidDataException {
        super.readExternal(element);
        getConfigurables().forEach(cfg -> cfg.readExternal(element));
    }

    @Override
    public void writeExternal(@NotNull Element element) {
        super.writeExternal(element);
        getConfigurables().forEach(cfg -> cfg.writeExternal(element));
    }

    public abstract List<String> buildCommandLineArgs(boolean debug) throws ExecutionException;

    public boolean emulateTerminal() {
        return pty.value;
    }

    @Override
    public T clone() {
        val myClone = (ZigExecConfigBase<?>) super.clone();
        myClone.workingDirectory = workingDirectory.clone();
        myClone.pty = pty.clone();
        return (T) myClone;
    }

    @Override
    public abstract @Nullable @NlsActions.ActionText String suggestedName();

    @Override
    public abstract @Nullable ProfileStateBase<T> getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment)
            throws ExecutionException;

    public @NotNull List<ZigConfigEditor.ZigConfigurable<?>> getConfigurables() {
        return List.of(workingDirectory, pty);
    }
}
