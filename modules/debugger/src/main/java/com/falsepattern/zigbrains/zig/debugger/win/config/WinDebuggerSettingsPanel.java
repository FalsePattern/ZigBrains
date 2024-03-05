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

import com.falsepattern.zigbrains.common.util.StringUtil;
import com.falsepattern.zigbrains.common.util.TextFieldUtil;
import com.falsepattern.zigbrains.project.openapi.MyDisposable;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.dsl.builder.AlignX;
import com.intellij.ui.dsl.builder.AlignY;
import com.intellij.ui.dsl.builder.Panel;
import lombok.Getter;
import lombok.val;
import org.jetbrains.annotations.Nullable;

import javax.swing.JLabel;
import java.util.Optional;

public class WinDebuggerSettingsPanel implements MyDisposable {
    @Getter
    private boolean disposed = false;

    public record SettingsData(@Nullable String cppToolsPath) {}

    @SuppressWarnings("DialogTitleCapitalization")
    private final TextFieldWithBrowseButton pathToArchive = TextFieldUtil.pathToFileTextField(this,
                                                                                              "Path to \"cpptools-win**.vsix\"",
                                                                                              () -> {});

    public SettingsData getData() {
        return new SettingsData(StringUtil.blankToNull(pathToArchive.getText()));
    }

    public void setData(SettingsData value) {
        pathToArchive.setText(Optional.ofNullable(value.cppToolsPath()).orElse(""));
    }

    private static HyperlinkLabel link(String url) {
        val href = new HyperlinkLabel(url);
        href.setHyperlinkTarget(url);
        return href;
    }

    public void attachPanelTo(Panel panel) {
        panel.panel(p -> {
            p.row("Debugging Zig on Windows requires you to manually install the MSVC toolchain.",
                  (r) -> null);

            p.row("To install the MSVC debugger, follow setup 3 under Prerequisites on the following page:",
                  (r) -> null);
            return null;
        });
        panel.panel(p -> {
            p.row((JLabel) null, (r) -> {
                r.cell(link("https://code.visualstudio.com/docs/cpp/config-msvc"));
                return null;
            });
            return null;
        });
        panel.panel(p -> {
            p.row("After you've installed MSVC, you also need download the vscode plugin with the debugger adapter.", (r) -> null);
            p.row("Latest known working version: 1.19.6. Newer versions may or may not work.", (r) -> null);
            return null;
        });
        panel.panel(p -> {
            p.row("You can download the latest version here:", (r) -> {
                r.cell(link("https://github.com/microsoft/vscode-cpptools/releases"));
                return null;
            });
            p.row("Put the path to the downloaded file here:", (r) -> {
                r.cell(pathToArchive).resizableColumn().align(AlignX.FILL).align(AlignY.FILL);
                return null;
            });
            return null;
        });
    }

    @Override
    public void dispose() {
        disposed = true;
        Disposer.dispose(pathToArchive);
    }
}
