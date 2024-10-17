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

package com.falsepattern.zigbrains.project.ide.newproject;

import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.Disposer;
import com.intellij.platform.ProjectGeneratorPeer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

import static com.falsepattern.zigbrains.common.util.dsl.JavaPanel.newPanel;

public class ZigProjectGeneratorPeer implements ProjectGeneratorPeer<ZigProjectConfigurationData> {
    private final ZigNewProjectPanel newProjectPanel;
    private volatile JComponent myComponent;

    public ZigProjectGeneratorPeer(boolean handleGit) {
        newProjectPanel = new ZigNewProjectPanel(handleGit);
    }

    @Override
    public @NotNull JComponent getComponent(@NotNull TextFieldWithBrowseButton myLocationField, @NotNull Runnable checkValid) {
        return createComponent();
    }

    @Override
    public void buildUI(@NotNull SettingsStep settingsStep) {
    }

    @Override
    public @NotNull ZigProjectConfigurationData getSettings() {
        return newProjectPanel.getData();
    }

    @Override
    public @Nullable ValidationInfo validate() {
        return null;
    }

    @Override
    public boolean isBackgroundJobRunning() {
        return false;
    }

    public @NotNull JComponent createComponent() {
        if (myComponent == null) {
            synchronized (this) {
                if (myComponent == null) {
                    return myComponent = newPanel(newProjectPanel::attachPanelTo);
                }
            }
        }
        return myComponent;
    }

    public void dispose() {
        Disposer.dispose(newProjectPanel);
    }
}
