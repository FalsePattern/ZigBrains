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

import com.falsepattern.zigbrains.common.util.ApplicationUtil;
import com.falsepattern.zigbrains.project.ide.util.projectwizard.ZigModuleBuilder;
import com.falsepattern.zigbrains.project.platform.ZigProjectGeneratorPeer;
import com.falsepattern.zigbrains.zig.Icons;
import com.intellij.ide.wizard.AbstractNewProjectWizardStep;
import com.intellij.ide.wizard.GitNewProjectWizardData;
import com.intellij.ide.wizard.NewProjectWizardStep;
import com.intellij.ide.wizard.language.LanguageGeneratorNewProjectWizard;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.ui.dsl.builder.AlignX;
import com.intellij.ui.dsl.builder.Panel;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.JLabel;
import java.io.IOException;
import java.nio.file.Path;

public class ZigNewProjectWizard implements LanguageGeneratorNewProjectWizard {
    @NotNull
    @Override
    public String getName() {
        return "Zig";
    }

    @Override
    public int getOrdinal() {
        return 900;
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return Icons.ZIG;
    }

    @NotNull
    @Override
    public NewProjectWizardStep createStep(@NotNull NewProjectWizardStep parent) {
        return new ZigNewProjectWizardStep(parent);
    }

    private static class ZigNewProjectWizardStep extends AbstractNewProjectWizardStep {
        private final ZigProjectGeneratorPeer peer = new ZigProjectGeneratorPeer();

        public ZigNewProjectWizardStep(@NotNull NewProjectWizardStep parentStep) {
            super(parentStep);
        }

        @Override
        public void setupUI(@NotNull Panel builder) {
            builder.row((JLabel) null, (r) -> {
                r.cell(peer.getComponent())
                 .align(AlignX.FILL);
                return null;
            });
        }

        @Override
        public void setupProject(@NotNull Project project) {
            val builder = new ZigModuleBuilder();
            val modList = builder.commit(project);
            if (modList == null || modList.isEmpty()) {
                return;
            }
            val module = modList.get(0);

            ModuleRootModificationUtil.updateModel(module, rootModel -> {
                builder.configurationData = peer.getSettings();
                builder.createProject(rootModel, "none");
                var gitData = GitNewProjectWizardData.Companion.getGitData(this);
                if (gitData == null) {
                    return;
                }
                if (gitData.getGit()) {
                    ApplicationUtil.writeAction(() -> createGitIgnoreFile(getContext().getProjectDirectory(), module));
                }
            });
        }

        private static final String GITIGNORE = ".gitignore";

        private static void createGitIgnoreFile(Path projectDir, Module module) {
            try {
                val directory = VfsUtil.createDirectoryIfMissing(projectDir.toString());
                if (directory == null) {
                    return;
                }
                val existingFile = directory.findChild(GITIGNORE);
                if (existingFile != null) {
                    return;
                }
                directory.createChildData(module, GITIGNORE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
