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

package com.falsepattern.zigbrains.project.execution.configurations.ui;

import com.falsepattern.zigbrains.project.execution.configurations.ZigRunExecutionConfiguration;
import com.falsepattern.zigbrains.project.ui.ZigCommandLinePanel;
import com.intellij.ui.dsl.builder.AlignX;
import com.intellij.ui.dsl.builder.AlignY;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;

import static com.intellij.ui.dsl.builder.BuilderKt.panel;

public class ZigRunExecutionConfigurationEditor extends AbstractZigExecutionConfigurationEditor<ZigRunExecutionConfiguration> {
    private JPanel panel = null;
    @Getter
    private final ZigCommandLinePanel commandLinePanel = new ZigCommandLinePanel();

    @Override
    protected @NotNull JComponent createEditor() {
        return panel = panel((p) -> {
            p.row("Run Config Command", (r) -> {
                r.cell(commandLinePanel)
                        .resizableColumn()
                        .align(AlignX.FILL)
                        .align(AlignY.FILL);
                return null;
            });
            p.row(workingDirectoryComponent.getLabel(), (r) -> {
                r.cell(workingDirectoryComponent)
                        .resizableColumn()
                        .align(AlignX.FILL)
                        .align(AlignY.FILL);
                return null;
            });
            return null;
        });
    }
}
