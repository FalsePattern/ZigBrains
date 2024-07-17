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

import com.falsepattern.zigbrains.common.SubConfigurable;
import com.falsepattern.zigbrains.common.util.dsl.JavaPanel;
import com.falsepattern.zigbrains.zig.lsp.ZLSStartupActivity;
import com.intellij.openapi.project.Project;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandles;

public class ZLSSettingsConfigurable implements SubConfigurable {
    private ZLSSettingsPanel appSettingsComponent;

    private final Project project;

    public ZLSSettingsConfigurable(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public void createComponent(JavaPanel panel) {
        appSettingsComponent = new ZLSSettingsPanel();
        appSettingsComponent.attachPanelTo(panel);
    }

    @Override
    public boolean isModified() {
        var settings = ZLSProjectSettingsService.getInstance(project);
        val data = appSettingsComponent.getData();
        return settings.isModified(data);
    }

    @Override
    public void apply() {
        var settings = ZLSProjectSettingsService.getInstance(project);
        val data = appSettingsComponent.getData();
        boolean reloadZLS = settings.zlsSettingsModified(data);
        settings.loadState(data);
        if (reloadZLS) {
            ZLSStartupActivity.startLSP(project, true);
        }
    }

    @Override
    public void reset() {
        var settings = ZLSProjectSettingsService.getInstance(project);
        appSettingsComponent.setData(settings.getState());
    }

    @Override
    public void disposeUIResources() {
        appSettingsComponent = null;
    }
}
