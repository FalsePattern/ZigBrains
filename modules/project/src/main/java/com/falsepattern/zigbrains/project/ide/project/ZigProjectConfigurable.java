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

package com.falsepattern.zigbrains.project.ide.project;

import com.falsepattern.zigbrains.common.SubConfigurable;
import com.falsepattern.zigbrains.common.util.dsl.JavaPanel;
import com.falsepattern.zigbrains.project.openapi.components.ZigProjectSettingsService;
import com.falsepattern.zigbrains.zig.lsp.ZLSStartupActivity;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import lombok.val;
import org.jetbrains.annotations.NotNull;

public class ZigProjectConfigurable implements SubConfigurable {
    private ZigProjectSettingsPanel settingsPanel;

    private final Project project;

    public ZigProjectConfigurable(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public void createComponent(JavaPanel panel) {
        settingsPanel = new ZigProjectSettingsPanel();
        settingsPanel.attachPanelTo(panel);
    }

    @Override
    public boolean isModified() {
        return ZigProjectSettingsService.getInstance(project).isModified(settingsPanel.getData());
    }

    @Override
    public void apply() throws ConfigurationException {
        val service = ZigProjectSettingsService.getInstance(project);
        val data = settingsPanel.getData();
        val modified = service.isModified(data);
        service.loadState(data);
        if (modified) {
            ZLSStartupActivity.initZLS(project);
        }
    }

    @Override
    public void reset() {
        val zigSettings = ZigProjectSettingsService.getInstance(project);
        settingsPanel.setData(zigSettings.getState());
    }

    @Override
    public void disposeUIResources() {
        Disposer.dispose(settingsPanel);
        settingsPanel = null;
    }
}
