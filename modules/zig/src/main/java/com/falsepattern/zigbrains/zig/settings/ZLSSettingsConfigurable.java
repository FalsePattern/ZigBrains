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

package com.falsepattern.zigbrains.zig.settings;

import com.falsepattern.zigbrains.zig.lsp.ZLSStartupActivity;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

public class ZLSSettingsConfigurable implements Configurable {
    private ZLSSettingsComponent appSettingsComponent;

    private final Project project;

    public ZLSSettingsConfigurable(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public String getDisplayName() {
        return "Zig";
    }

    @Override
    public @Nullable JComponent createComponent() {
        appSettingsComponent = new ZLSSettingsComponent();
        return appSettingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        var settings = ZLSSettingsState.getInstance(project);
        boolean modified = zlsSettingsModified(settings);
        modified |= settings.asyncFolding != appSettingsComponent.getAsyncFolding();
        return modified;
    }

    private boolean zlsSettingsModified(ZLSSettingsState settings) {
        boolean modified = !settings.zlsPath.equals(appSettingsComponent.getZLSPath());
        modified |= !settings.zlsConfigPath.equals(appSettingsComponent.getZLSConfigPath());
        modified |= settings.debug != appSettingsComponent.getDebug();
        modified |= settings.messageTrace != appSettingsComponent.getMessageTrace();
        modified |= settings.increaseTimeouts != appSettingsComponent.getIncreaseTimeouts();
        return modified;
    }

    @Override
    public void apply() {
        var settings = ZLSSettingsState.getInstance(project);
        boolean reloadZLS = zlsSettingsModified(settings);
        settings.zlsPath = appSettingsComponent.getZLSPath();
        settings.zlsConfigPath = appSettingsComponent.getZLSConfigPath();
        settings.asyncFolding = appSettingsComponent.getAsyncFolding();
        settings.debug = appSettingsComponent.getDebug();
        settings.messageTrace = appSettingsComponent.getMessageTrace();
        settings.increaseTimeouts = appSettingsComponent.getIncreaseTimeouts();
        if (reloadZLS) {
            ZLSStartupActivity.initZLS(project);
        }
    }

    @Override
    public void reset() {
        var settings = ZLSSettingsState.getInstance(project);
        appSettingsComponent.setZLSPath(settings.zlsPath);
        appSettingsComponent.setZLSConfigPath(settings.zlsConfigPath);
        appSettingsComponent.setDebug(settings.debug);
        appSettingsComponent.setAsyncFolding(settings.asyncFolding);
        appSettingsComponent.setMessageTrace(settings.messageTrace);
        appSettingsComponent.setIncreaseTimeouts(settings.increaseTimeouts);
        appSettingsComponent.setAsyncFolding(settings.asyncFolding);
    }

    @Override
    public void disposeUIResources() {
        appSettingsComponent = null;
    }
}
