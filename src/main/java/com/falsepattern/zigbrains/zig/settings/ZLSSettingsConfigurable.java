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

package com.falsepattern.zigbrains.zig.settings;

import com.falsepattern.zigbrains.settings.AbstractConfigurable;
import com.falsepattern.zigbrains.zig.lsp.ZLSStartupActivity;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandles;
import java.util.Set;

public class ZLSSettingsConfigurable extends AbstractConfigurable<ZLSSettingsState> {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();

    private static final Set<String> RELOAD_CONFIGS = Set.of("zlsPath", "zlsConfigPath", "increaseTimeouts", "debug", "messageTrace");

    private final Project project;

    public ZLSSettingsConfigurable(@NotNull Project project) throws IllegalAccessException {
        super(LOOKUP, "Zig", ZLSSettingsState.class);
        this.project = project;
    }

    @Override
    public void apply() throws ConfigurationException {
        boolean reloadZLS = zlsSettingsModified();
        super.apply();
        if (reloadZLS) {
            ZLSStartupActivity.initZLS(project);
        }
    }

    private boolean zlsSettingsModified() {
        if (configurableGui != null) {
            return configurableGui.modified(getHolder(), false, RELOAD_CONFIGS);
        }
        return false;
    }

    @Override
    protected ZLSSettingsState getHolder() {
        return ZLSSettingsState.getInstance(project);
    }
}
