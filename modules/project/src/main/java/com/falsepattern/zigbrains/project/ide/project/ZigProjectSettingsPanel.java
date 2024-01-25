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

import com.falsepattern.zigbrains.common.util.StringUtil;
import com.falsepattern.zigbrains.common.util.TextFieldUtil;
import com.falsepattern.zigbrains.project.openapi.MyDisposable;
import com.falsepattern.zigbrains.project.openapi.UIDebouncer;
import com.falsepattern.zigbrains.project.openapi.components.ZigProjectSettingsService;
import com.falsepattern.zigbrains.project.toolchain.AbstractZigToolchain;
import com.falsepattern.zigbrains.project.toolchain.ZigToolchainProvider;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.ui.JBColor;
import com.intellij.ui.dsl.builder.AlignX;
import com.intellij.ui.dsl.builder.Panel;
import lombok.Getter;
import lombok.val;
import org.jetbrains.annotations.Nullable;

import javax.swing.JLabel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class ZigProjectSettingsPanel implements MyDisposable {
    @Getter
    private boolean disposed = false;

    public record SettingsData(@Nullable String explicitPathToStd,
                               @Nullable AbstractZigToolchain toolchain) {}

    private final UIDebouncer versionUpdateDebouncer = new UIDebouncer(this);

    private final ZigToolchainPathChooserComboBox toolchainPathChooserComboBox = new ZigToolchainPathChooserComboBox(this::updateUI);

    private final JLabel toolchainVersion = new JLabel();

    private final TextFieldWithBrowseButton pathToStdField = TextFieldUtil.pathToDirectoryTextField(this,
                                                                                                    "Path to Standard Library",
                                                                                                    () -> {});

    public SettingsData getData() {
        val toolchain = Optional.ofNullable(toolchainPathChooserComboBox.getSelectedPath())
                                .map(ZigToolchainProvider::findToolchain)
                                .orElse(null);
        return new SettingsData(StringUtil.blankToNull(pathToStdField.getText()), toolchain);
    }

    public void setData(SettingsData value) {
        toolchainPathChooserComboBox.setSelectedPath(Optional.ofNullable(value.toolchain()).map(tc -> tc.location).orElse(null));

        pathToStdField.setText(Optional.ofNullable(value.explicitPathToStd()).orElse(""));

        updateUI();
    }

    public void attachPanelTo(Panel panel) {
        setData(new SettingsData(null,
                                 Optional.ofNullable(ProjectManager.getInstance()
                                                                   .getDefaultProject()
                                                                   .getService(ZigProjectSettingsService.class))
                                         .map(ZigProjectSettingsService::getToolchain)
                                         .orElse(AbstractZigToolchain.suggest(Paths.get(".")))));

        panel.row("Toolchain Location", (r) -> {
            r.cell(toolchainPathChooserComboBox)
             .align(AlignX.FILL);

            return null;
        });

        panel.row("Toolchain Version", (r) -> {
            r.cell(toolchainVersion);
            return null;
        });

        panel.row("Standard Library Location", (r) -> {
            r.cell(pathToStdField)
             .align(AlignX.FILL);
            return null;
        });
    }

    @Override
    public void dispose() {
        disposed = true;
        Disposer.dispose(toolchainPathChooserComboBox);
    }

    private void updateUI() {
        val pathToToolchain = toolchainPathChooserComboBox.getSelectedPath();

        versionUpdateDebouncer.run(
                () -> {
                    val toolchain = Optional.ofNullable(pathToToolchain).map(ZigToolchainProvider::findToolchain).orElse(null);
                    val zig = Optional.ofNullable(toolchain).map(AbstractZigToolchain::zig).orElse(null);
                    val version = Optional.ofNullable(zig).flatMap(z -> z.queryVersion(Path.of("."))).orElse(null);
                    val stdPath = Optional.ofNullable(zig).flatMap(z -> z.getStdPath(Path.of("."))).orElse(null);

                    return new Pair<>(version, stdPath);
                    },
                (pair) -> {
                    val zigVersion = pair.first;
                    val stdPath = pair.second;
                    toolchainVersion.setText(StringUtil.orEmpty(zigVersion));
                    toolchainVersion.setForeground(JBColor.foreground());

                    pathToStdField.setText(StringUtil.orEmpty(stdPath));
                });
    }
}
