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

package com.falsepattern.zigbrains.project.execution.common;

import com.falsepattern.zigbrains.project.execution.base.ZigExecConfigBase;
import com.falsepattern.zigbrains.project.ui.WorkingDirectoryComponent;
import com.falsepattern.zigbrains.project.ui.ZigFilePathPanel;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.dsl.builder.AlignX;
import com.intellij.ui.dsl.builder.AlignY;
import com.intellij.ui.dsl.builder.Panel;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.nio.file.Paths;
import java.util.Objects;

import static com.intellij.ui.dsl.builder.BuilderKt.panel;

public class ZigConfigEditor<T extends ZigExecConfigBase<T>> extends SettingsEditor<T> {
    protected final LabeledComponent<TextFieldWithBrowseButton> workingDirectoryComponent =
            new WorkingDirectoryComponent();

    @Override
    protected void applyEditorTo(@NotNull T s) throws ConfigurationException {
        s.workingDirectory = Paths.get(workingDirectoryComponent.getComponent().getText());
    }

    @Override
    protected void resetEditorFrom(@NotNull T s) {
        workingDirectoryComponent.getComponent().setText(Objects.requireNonNullElse(s.workingDirectory, "").toString());
    }

    @Override
    protected final @NotNull JComponent createEditor() {
        return panel((p) -> {
            constructPanel(p);
            return null;
        });
    }

    protected void constructPanel(Panel p) {
        p.row(workingDirectoryComponent.getLabel(), (r) -> {
            r.cell(workingDirectoryComponent).resizableColumn().align(AlignX.FILL).align(AlignY.FILL);
            return null;
        });
    }

    public static abstract class WithFilePath<T extends ZigExecConfigBase<T>> extends ZigConfigEditor<T> {
        private final ZigFilePathPanel filePathPanel = new ZigFilePathPanel();

        @Override
        protected void applyEditorTo(@NotNull T s) throws ConfigurationException {
            super.applyEditorTo(s);
            setFilePath(s, filePathPanel.getText());
        }

        @Override
        protected void resetEditorFrom(@NotNull T s) {
            super.resetEditorFrom(s);
            filePathPanel.setText(Objects.requireNonNullElse(getFilePath(s), ""));
        }

        @Override
        protected void constructPanel(Panel p) {
            super.constructPanel(p);
            p.row("Target file", (r) -> {
                r.cell(filePathPanel).resizableColumn().align(AlignX.FILL).align(AlignY.FILL);
                return null;
            });
        }

        protected abstract String getFilePath(T config);
        protected abstract void setFilePath(T config, String path);
    }
}
