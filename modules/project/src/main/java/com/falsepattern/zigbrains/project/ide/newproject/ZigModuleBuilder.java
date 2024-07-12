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

package com.falsepattern.zigbrains.project.ide.newproject;

import com.falsepattern.zigbrains.project.openapi.module.ZigModuleType;
import com.falsepattern.zigbrains.project.util.ExperimentUtil;
import com.intellij.ide.NewProjectWizardLegacy;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.ui.JBUI;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

public class ZigModuleBuilder extends ModuleBuilder {
    public @Nullable ZigProjectConfigurationData configurationData = null;
    public boolean forceGitignore = false;

    @Override
    public ModuleType<?> getModuleType() {
        return ZigModuleType.INSTANCE;
    }

    @Override
    public boolean isAvailable() {
        return NewProjectWizardLegacy.isAvailable();
    }

    @Override
    public void setupRootModel(@NotNull ModifiableRootModel modifiableRootModel) {
        createProject(modifiableRootModel);
    }

    @Override
    public @Nullable ModuleWizardStep getCustomOptionsStep(WizardContext context, Disposable parentDisposable) {
        val step = new ZigModuleWizardStep();
        Disposer.register(parentDisposable, step::disposeUIResources);
        return step;
    }

    public void createProject(ModifiableRootModel rootModel) {
        val contentEntry = doAddContentEntry(rootModel);
        if (contentEntry == null) {
            return;
        }
        val root = contentEntry.getFile();
        if (root == null) {
            return;
        }
        if (configurationData == null) {
            return;
        }
        configurationData.generateProject(this, rootModel.getProject(), root, forceGitignore);
        root.refresh(false, true);
    }

    public class ZigModuleWizardStep extends ModuleWizardStep {
        private final ZigProjectGeneratorPeer peer = new ZigProjectGeneratorPeer(true);

        @Override
        public JComponent getComponent() {
            return withBorderIfNeeded(peer.getComponent());
        }

        @Override
        public void disposeUIResources() {
            peer.dispose();
        }

        @Override
        public void updateDataModel() {
            ZigModuleBuilder.this.configurationData = peer.getSettings();
        }

        private <T extends JComponent> T withBorderIfNeeded(T component) {
            if (ExperimentUtil.isNewWizard()) {
                component.setBorder(JBUI.Borders.empty(14, 20));
            }
            return component;
        }
    }
}
