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

package com.falsepattern.zigbrains.project.ide.project;

import com.falsepattern.zigbrains.project.openapi.components.ZigProjectSettingsService;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.NlsContexts;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.util.Objects;

import static com.intellij.ui.dsl.builder.BuilderKt.panel;

public class ZigProjectConfigurable implements Configurable {
    private ZigProjectSettingsPanel settingsPanel;

    private final Project project;

    public ZigProjectConfigurable(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "Zig";
    }

    @Override
    public @Nullable JComponent createComponent() {
        settingsPanel = new ZigProjectSettingsPanel();
        return panel((p) -> {
            settingsPanel.attachPanelTo(p);
            return null;
        });
    }

    @Override
    public boolean isModified() {
        var zigSettings = ZigProjectSettingsService.getInstance(project);
        var settingsData = settingsPanel.getData();
        return !Objects.equals(settingsData.toolchain(), zigSettings.getToolchain()) ||
               !Objects.equals(settingsData.explicitPathToStd(), zigSettings.getExplicitPathToStd());
    }

    @Override
    public void apply() throws ConfigurationException {
        val zigSettings = ZigProjectSettingsService.getInstance(project);
        val settingsData = settingsPanel.getData();
        zigSettings.modify((settings) -> {
            settings.setToolchain(settingsData.toolchain());
            settings.setExplicitPathToStd(settingsData.explicitPathToStd());
        });
    }

    @Override
    public void reset() {
        val zigSettings = ZigProjectSettingsService.getInstance(project);
        settingsPanel.setData(new ZigProjectSettingsPanel.SettingsData(
                zigSettings.getExplicitPathToStd(),
                zigSettings.getToolchain()
        ));
    }

    @Override
    public void disposeUIResources() {
        Disposer.dispose(settingsPanel);
        settingsPanel = null;
    }
}
