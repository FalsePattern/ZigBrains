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

package com.falsepattern.zigbrains.project.execution.run.config;

import com.falsepattern.zigbrains.project.execution.base.config.EditorBase;
import com.falsepattern.zigbrains.project.ui.ZigCommandLinePanel;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.dsl.builder.AlignX;
import com.intellij.ui.dsl.builder.AlignY;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;

import java.util.Objects;

import static com.intellij.ui.dsl.builder.BuilderKt.panel;

public class EditorRun extends EditorBase<ZigExecConfigRun> {
    @Getter
    private final ZigCommandLinePanel commandLinePanel = new ZigCommandLinePanel();

    @Override
    protected void applyEditorTo(@NotNull ZigExecConfigRun s) throws ConfigurationException {
        super.applyEditorTo(s);
        s.filePath = commandLinePanel.getText();
    }

    @Override
    protected void resetEditorFrom(@NotNull ZigExecConfigRun s) {
        super.resetEditorFrom(s);
        commandLinePanel.setText(Objects.requireNonNullElse(s.filePath, ""));
    }

    @Override
    protected @NotNull JComponent createEditor() {
        return panel((p) -> {
            p.row("Target file", (r) -> {
                r.cell(commandLinePanel).resizableColumn().align(AlignX.FILL).align(AlignY.FILL);
                return null;
            });
            p.row(workingDirectoryComponent.getLabel(), (r) -> {
                r.cell(workingDirectoryComponent).resizableColumn().align(AlignX.FILL).align(AlignY.FILL);
                return null;
            });
            return null;
        });
    }
}
