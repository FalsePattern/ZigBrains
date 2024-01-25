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

package com.falsepattern.zigbrains.project.ide.util.projectwizard;

import com.falsepattern.zigbrains.project.ide.newproject.ZigProjectConfigurationData;
import com.falsepattern.zigbrains.project.openapi.module.ZigModuleType;
import com.intellij.ide.NewProjectWizardLegacy;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.util.Disposer;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ZigModuleBuilder extends ModuleBuilder {
    public @Nullable ZigProjectConfigurationData configurationData = null;

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
        createProject(modifiableRootModel, "git");
    }

    @Override
    public @Nullable ModuleWizardStep getCustomOptionsStep(WizardContext context, Disposable parentDisposable) {
        val step = new ZigModuleWizardStep();
        Disposer.register(parentDisposable, step::disposeUIResources);
        return step;
    }

    public void createProject(ModifiableRootModel modifiableRootModel, @Nullable String vcs) {
        val contentEntry = doAddContentEntry(modifiableRootModel);
        if (contentEntry == null) {
            return;
        }
        val root = contentEntry.getFile();
        if (root == null) {
            return;
        }
        modifiableRootModel.inheritSdk();
        root.refresh(false, true);
    }
}
