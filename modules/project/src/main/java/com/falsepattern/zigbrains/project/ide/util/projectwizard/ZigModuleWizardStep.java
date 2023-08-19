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

package com.falsepattern.zigbrains.project.ide.util.projectwizard;

import com.falsepattern.zigbrains.project.ide.newproject.ZigNewProjectPanel;
import com.falsepattern.zigbrains.project.util.ExperimentUtil;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.ui.JBUI;

import javax.swing.JComponent;

import static com.intellij.ui.dsl.builder.BuilderKt.panel;

public class ZigModuleWizardStep extends ModuleWizardStep {
    private final ZigNewProjectPanel newProjectPanel = new ZigNewProjectPanel();

    @Override
    public JComponent getComponent() {
        return withBorderIfNeeded(panel((p) -> {
            newProjectPanel.attachPanelTo(p);
            return null;
        }));
    }

    @Override
    public void disposeUIResources() {
        Disposer.dispose(newProjectPanel);
    }

    @Override
    public void updateDataModel() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private <T extends JComponent> T withBorderIfNeeded(T component) {
        if (ExperimentUtil.isNewWizard()) {
            component.setBorder(JBUI.Borders.empty(14, 20));
        }
        return component;
    }
}
