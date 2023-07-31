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

package com.falsepattern.zigbrains.settings;

import com.falsepattern.zigbrains.lsp.ZLSStartupActivity;
import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

public class AppSettingsConfigurable implements Configurable {
    private AppSettingsComponent appSettingsComponent;

    @Override
    public String getDisplayName() {
        return "Zig";
    }

    @Override
    public @Nullable JComponent createComponent() {
        appSettingsComponent = new AppSettingsComponent();
        return appSettingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        var settings = AppSettingsState.getInstance();
        boolean modified = !settings.zlsPath.equals(appSettingsComponent.getZLSPath());
        modified |= !settings.zlsConfigPath.equals(appSettingsComponent.getZLSConfigPath());
        modified |= settings.debug != appSettingsComponent.getDebug();
        modified |= settings.messageTrace != appSettingsComponent.getMessageTrace();
        modified |= settings.increaseTimeouts != appSettingsComponent.getIncreaseTimeouts();
        return modified;
    }

    @Override
    public void apply() {
        var settings = AppSettingsState.getInstance();
        settings.zlsPath = appSettingsComponent.getZLSPath();
        settings.zlsConfigPath = appSettingsComponent.getZLSConfigPath();
        settings.debug = appSettingsComponent.getDebug();
        settings.messageTrace = appSettingsComponent.getMessageTrace();
        settings.increaseTimeouts = appSettingsComponent.getIncreaseTimeouts();
        ZLSStartupActivity.initZLS();
    }

    @Override
    public void reset() {
        var settings = AppSettingsState.getInstance();
        appSettingsComponent.setZLSPath(settings.zlsPath);
        appSettingsComponent.setZLSConfigPath(settings.zlsConfigPath);
        appSettingsComponent.setDebug(settings.debug);
        appSettingsComponent.setMessageTrace(settings.messageTrace);
        appSettingsComponent.setIncreaseTimeouts(settings.increaseTimeouts);
    }

    @Override
    public void disposeUIResources() {
        appSettingsComponent = null;
    }
}
