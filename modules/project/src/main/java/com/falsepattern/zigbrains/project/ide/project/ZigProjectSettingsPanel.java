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

import com.falsepattern.zigbrains.common.util.PathUtil;
import com.falsepattern.zigbrains.common.util.StringUtil;
import com.falsepattern.zigbrains.common.util.TextFieldUtil;
import com.falsepattern.zigbrains.common.util.dsl.JavaPanel;
import com.falsepattern.zigbrains.project.openapi.MyDisposable;
import com.falsepattern.zigbrains.project.openapi.UIDebouncer;
import com.falsepattern.zigbrains.project.openapi.components.ZigProjectSettings;
import com.falsepattern.zigbrains.project.openapi.components.ZigProjectSettingsService;
import com.falsepattern.zigbrains.project.toolchain.AbstractZigToolchain;
import com.falsepattern.zigbrains.project.toolchain.ZigToolchainProvider;
import com.falsepattern.zigbrains.project.toolchain.tools.ZigCompilerTool;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.dsl.builder.AlignX;
import lombok.Getter;
import lombok.val;

import javax.swing.JLabel;
import java.awt.event.ActionEvent;
import java.util.Optional;

import static com.falsepattern.zigbrains.common.util.KtUtil.$f;

public class ZigProjectSettingsPanel implements MyDisposable {
    @Getter
    private boolean disposed = false;

    private final UIDebouncer versionUpdateDebouncer = new UIDebouncer(this);

    private final TextFieldWithBrowseButton pathToToolchain = TextFieldUtil.pathToDirectoryTextField(this,
                                                                                                     "Path to the Zig Toolchain",
                                                                                                     this::updateUI);

    private final JLabel toolchainVersion = new JLabel();

    private final JBCheckBox stdFieldOverride = new JBCheckBox("Override");

    private final TextFieldWithBrowseButton pathToStdField = TextFieldUtil.pathToDirectoryTextField(this,
                                                                                                    "Path to Standard Library",
                                                                                                    () -> {});

    {
        stdFieldOverride.addChangeListener(e -> {
            if (stdFieldOverride.isSelected()) {
                pathToStdField.setEnabled(true);
            } else {
                pathToStdField.setEnabled(false);
                updateUI();
            }
        });
    }

    private void autodetect(ActionEvent e) {
        autodetect();
    }

    public void autodetect() {
        val tc = AbstractZigToolchain.suggest();
        if (tc != null) {
            pathToToolchain.setText(PathUtil.stringFromPath(tc.getLocation()));
            updateUI();
        }
    }

    public ZigProjectSettings getData() {
        val toolchain = Optional.of(pathToToolchain.getText())
                                .map(PathUtil::pathFromString)
                                .map(ZigToolchainProvider::findToolchain)
                                .orElse(null);
        return new ZigProjectSettings(stdFieldOverride.isSelected() ? StringUtil.blankToNull(pathToStdField.getText()) : null, toolchain);
    }

    public void setData(ZigProjectSettings value) {
        pathToToolchain.setText(Optional.ofNullable(value.getToolchainHomeDirectory())
                                        .orElse(""));

        stdFieldOverride.setSelected(value.overrideStdPath);

        pathToStdField.setText(Optional.ofNullable(value.getExplicitPathToStd()).orElse(""));

        pathToStdField.setEnabled(value.overrideStdPath);

        updateUI();
    }

    public void attachPanelTo(JavaPanel p) {
        Optional.ofNullable(ZigProjectSettingsService.getInstance(ProjectManager.getInstance().getDefaultProject()))
                .map(ZigProjectSettingsService::getState)
                .ifPresent(this::setData);
        p.group("Zig Settings", true, p2 -> {
            p2.row("Toolchain location", r -> {
                r.cell(pathToToolchain).resizableColumn().align(AlignX.FILL);
                r.button("Autodetect", $f(this::autodetect));
            });
            p2.cell("Toolchain version", toolchainVersion);
            p2.cell("Override standard library path", stdFieldOverride);
            p2.row("Standard library location", row -> {
                row.cell(pathToStdField).resizableColumn().align(AlignX.FILL);
                row.cell(stdFieldOverride);
            });
        });
    }

    @Override
    public void dispose() {
        disposed = true;
        Disposer.dispose(pathToToolchain);
    }

    private void updateUI() {
        val pathToToolchain = PathUtil.pathFromString(this.pathToToolchain.getText());

        versionUpdateDebouncer.run(
                () -> {
                    val toolchain = Optional.ofNullable(pathToToolchain).map(ZigToolchainProvider::findToolchain).orElse(null);
                    val zig = Optional.ofNullable(toolchain).map(AbstractZigToolchain::zig).orElse(null);
                    val version = Optional.ofNullable(zig).flatMap(ZigCompilerTool::queryVersion).orElse(null);
                    val stdPath = Optional.ofNullable(zig).flatMap(ZigCompilerTool::getStdPath).orElse(null);

                    return new Pair<>(version, stdPath);
                    },
                (pair) -> {
                    val zigVersion = pair.first;
                    val stdPath = pair.second;
                    toolchainVersion.setText(StringUtil.orEmpty(zigVersion));
                    toolchainVersion.setForeground(JBColor.foreground());

                    if (!stdFieldOverride.isSelected())
                        pathToStdField.setText(StringUtil.orEmpty(stdPath));
                });
    }
}
