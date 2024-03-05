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

package com.falsepattern.zigbrains.zig.debugger.win.config;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.JBColor;
import com.intellij.util.system.OS;
import lombok.val;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

import java.util.Objects;

import static com.intellij.ui.dsl.builder.BuilderKt.panel;

public class WinDebuggerConfigurable implements Configurable {
    private WinDebuggerSettingsPanel settingsPanel;

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "Zig Debugger (Windows)";
    }

    @Override
    public @Nullable JComponent createComponent() {
        settingsPanel = new WinDebuggerSettingsPanel();
        return panel((p) -> {
            if (OS.CURRENT != OS.Windows) {
                p.row("This menu has no effect on linux/macos/non-windows systems, use the C++ toolchains (see plugin description).", (r) -> null);
                p.row("For completeness' sake, here is what you would need to configure on Windows:", (r) -> null);
                p.separator(JBColor.foreground());
            }
            settingsPanel.attachPanelTo(p);
            return null;
        });
    }

    @Override
    public boolean isModified() {
        if (settingsPanel == null)
            return false;
        var cppSettings = WinDebuggerConfigService.getInstance();
        var settingsData = settingsPanel.getData();
        return !Objects.equals(settingsData.cppToolsPath(), cppSettings.cppToolsPath);
    }

    @Override
    public void apply() throws ConfigurationException {
        if (settingsPanel == null)
            return;
        var cppSettings = WinDebuggerConfigService.getInstance();
        var settingsData = settingsPanel.getData();
        cppSettings.cppToolsPath = settingsData.cppToolsPath();
    }

    @Override
    public void reset() {
        if (settingsPanel == null)
            return;
        val cppSettings = WinDebuggerConfigService.getInstance();
        settingsPanel.setData(new WinDebuggerSettingsPanel.SettingsData(cppSettings.cppToolsPath));
    }

    @Override
    public void disposeUIResources() {
        if (settingsPanel == null)
            return;
        Disposer.dispose(settingsPanel);
        settingsPanel = null;
    }
}
