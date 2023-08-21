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

package com.falsepattern.zigbrains.project.execution.configurations.ui;

import com.falsepattern.zigbrains.project.execution.configurations.AbstractZigExecutionConfiguration;
import com.falsepattern.zigbrains.project.ui.ZigCommandLinePanel;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;

public abstract class AbstractZigExecutionConfigurationEditor<T extends AbstractZigExecutionConfiguration> extends
        SettingsEditor<T> {
    public abstract ZigCommandLinePanel getCommandLinePanel();
    protected final LabeledComponent<TextFieldWithBrowseButton> workingDirectoryComponent = new WorkingDirectoryComponent();

    @Override
    protected void applyEditorTo(@NotNull T s) throws ConfigurationException {
        s.setCommand(getCommandLinePanel().getText());
        s.workingDirectory = Paths.get(workingDirectoryComponent.getComponent().getText());
    }

    @Override
    protected void resetEditorFrom(@NotNull T s) {
        getCommandLinePanel().setText(s.getCommand());
        if (s.workingDirectory == null) {
            workingDirectoryComponent.getComponent().setText("");
        } else {
            workingDirectoryComponent.getComponent().setText(s.workingDirectory.toString());
        }
    }
}
