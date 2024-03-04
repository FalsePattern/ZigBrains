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

package com.falsepattern.zigbrains.project.execution.base;

import com.falsepattern.zigbrains.project.ui.WorkingDirectoryComponent;
import com.falsepattern.zigbrains.project.ui.ZigFilePathPanel;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.dsl.builder.AlignX;
import com.intellij.ui.dsl.builder.AlignY;
import com.intellij.ui.dsl.builder.Panel;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.intellij.ui.dsl.builder.BuilderKt.panel;

public class ZigConfigEditor<T extends ZigExecConfigBase<T>> extends SettingsEditor<T> {
    private final List<ZigConfigModule<T>> modules;

    public ZigConfigEditor(List<ZigConfigModule<T>> modules) {
        this.modules = new ArrayList<>(modules);
    }

    @Override
    protected void applyEditorTo(@NotNull T s) throws ConfigurationException {
        for (val module: modules) {
            module.applyTo(s);
        }
    }

    @Override
    protected void resetEditorFrom(@NotNull T s) {
        for (val module: modules) {
            module.resetFrom(s);
        }
    }

    @Override
    protected final @NotNull JComponent createEditor() {
        return panel((p) -> {
            for (val module: modules) {
                module.construct(p);
            }
            return null;
        });
    }

    public interface ZigConfigModule<T> {
        void applyTo(@NotNull T s) throws ConfigurationException;
        void resetFrom(@NotNull T s);
        void construct(Panel p);
    }

    public static class WorkingDirectoryModule<T extends ZigExecConfigBase<T>> implements ZigConfigModule<T> {
        protected final LabeledComponent<TextFieldWithBrowseButton> workingDirectoryComponent =
                new WorkingDirectoryComponent();

        @Override
        public void applyTo(@NotNull T s) {
            s.workingDirectory = Paths.get(workingDirectoryComponent.getComponent().getText());
        }

        @Override
        public void resetFrom(@NotNull T s) {
            workingDirectoryComponent.getComponent().setText(Objects.requireNonNullElse(s.workingDirectory, "").toString());
        }

        @Override
        public void construct(Panel p) {
            p.row(workingDirectoryComponent.getLabel(), (r) -> {
                r.cell(workingDirectoryComponent).resizableColumn().align(AlignX.FILL).align(AlignY.FILL);
                return null;
            });
        }
    }

    public static class FilePathModule<T extends ZigExecConfigBase<T> & FilePathModule.Carrier> implements ZigConfigModule<T> {
        private final ZigFilePathPanel filePathPanel = new ZigFilePathPanel();

        @Override
        public void applyTo(@NotNull T s) {
            s.setFilePath(filePathPanel.getText());
        }

        @Override
        public void resetFrom(@NotNull T s) {
            filePathPanel.setText(Objects.requireNonNullElse(s.getFilePath(), ""));
        }

        @Override
        public void construct(Panel p) {
            p.row("Target file", (r) -> {
                r.cell(filePathPanel).resizableColumn().align(AlignX.FILL).align(AlignY.FILL);
                return null;
            });
        }

        public interface Carrier {
            void setFilePath(String path);
            String getFilePath();
        }
    }

    public static class ColoredModule<T extends ZigExecConfigBase<T> & ColoredModule.Carrier> implements ZigConfigModule<T> {
        private final JBCheckBox checkBox = new JBCheckBox();

        @Override
        public void applyTo(@NotNull T s) throws ConfigurationException {
            s.setColored(checkBox.isSelected());
        }

        @Override
        public void resetFrom(@NotNull T s) {
            checkBox.setSelected(s.isColored());
        }

        @Override
        public void construct(Panel p) {
            p.row("Colored terminal", (r) -> {
                r.cell(checkBox);
                return null;
            });
        }


        public interface Carrier {
            void setColored(boolean color);
            boolean isColored();
        }
    }
}
