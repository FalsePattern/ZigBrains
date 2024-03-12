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

package com.falsepattern.zigbrains.project.platform;

import com.falsepattern.zigbrains.project.ide.newproject.ZigProjectConfigurationData;
import com.falsepattern.zigbrains.project.ide.project.ZigDefaultTemplate;
import com.falsepattern.zigbrains.project.ide.util.projectwizard.ZigProjectSettingsStep;
import com.falsepattern.zigbrains.project.openapi.components.ZigProjectSettingsService;
import com.falsepattern.zigbrains.zig.Icons;
import com.intellij.facet.ui.ValidationResult;
import com.intellij.ide.util.projectWizard.AbstractNewProjectStep;
import com.intellij.ide.util.projectWizard.CustomStepProjectGenerator;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.impl.welcomeScreen.AbstractActionWithPanel;
import com.intellij.platform.DirectoryProjectGenerator;
import com.intellij.platform.ProjectGeneratorPeer;
import com.intellij.util.ResourceUtil;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ZigDirectoryProjectGenerator implements DirectoryProjectGenerator<ZigProjectConfigurationData>,
        CustomStepProjectGenerator<ZigProjectConfigurationData> {

    @Override
    public @NotNull @NlsContexts.Label String getName() {
        return "Zig";
    }

    @Override
    public @Nullable Icon getLogo() {
        return Icons.ZIG;
    }

    @Override
    public @NotNull ProjectGeneratorPeer<ZigProjectConfigurationData> createPeer() {
        return new ZigProjectGeneratorPeer();
    }

    @Override
    public @NotNull ValidationResult validate(@NotNull String baseDirPath) {
        return ValidationResult.OK;
    }

    private static String getResourceString(String path) throws IOException {
        byte[] data = ResourceUtil.getResourceAsBytes(path, ZigDirectoryProjectGenerator.class.getClassLoader());
        if (data == null)
            throw new IOException("Could not find resource " + path + "!");
        return new String(data, StandardCharsets.UTF_8);
    }

    @Override
    public void generateProject(@NotNull Project project, @NotNull VirtualFile baseDir, @NotNull ZigProjectConfigurationData data, @NotNull Module module) {
        val settings = data.settings();

        var svc = ZigProjectSettingsService.getInstance(project);
        val toolchain = settings.toolchain();
        svc.getState().setToolchain(toolchain);

        val template = data.selectedTemplate();

        if (template instanceof ZigDefaultTemplate.ZigInitTemplate) {
            if (toolchain == null) {
                Notifications.Bus.notify(new Notification("ZigBrains.Project",
                                                          "Tried to generate project with zig init, but zig toolchain is invalid!",
                                                          NotificationType.ERROR));
                return;
            }
            val zig = toolchain.zig();
            val resultOpt = zig.callWithArgs(baseDir.toNioPath(), 10000, "init");
            if (resultOpt.isEmpty()) {
                Notifications.Bus.notify(new Notification("ZigBrains.Project",
                                                          "Failed to invoke \"zig init\"!",
                                                          NotificationType.ERROR));
                return;
            }
            val result = resultOpt.get();
            if (result.getExitCode() != 0) {
                Notifications.Bus.notify(new Notification("ZigBrains.Project",
                                                          "\"zig init\" failed with exit code " + result.getExitCode() + "! Check the IDE log files!",
                                                          NotificationType.ERROR));
                System.err.println(result.getStderr());
            }
        } else {
            try {
                val projectName = project.getName();
                WriteAction.run(() -> {
                    for (val fileTemplate : template.fileTemplates().entrySet()) {
                        var fileName = fileTemplate.getKey();
                        VirtualFile parentDir;
                        if (fileName.contains("/")) {
                            val slashIndex = fileName.indexOf('/');
                            parentDir = baseDir.createChildDirectory(this, fileName.substring(0, slashIndex));
                            fileName = fileName.substring(slashIndex + 1);
                        } else {
                            parentDir = baseDir;
                        }
                        val templateDir = fileTemplate.getValue();
                        val resourceData = getResourceString("project-gen/" + templateDir + "/" + fileName + ".template").replace("@@PROJECT_NAME@@", projectName);
                        val targetFile = parentDir.createChildData(this, fileName);
                        VfsUtil.saveText(targetFile, resourceData);
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public AbstractActionWithPanel createStep(DirectoryProjectGenerator<ZigProjectConfigurationData> projectGenerator, AbstractNewProjectStep.AbstractCallback<ZigProjectConfigurationData> callback) {
        return new ZigProjectSettingsStep(projectGenerator);
    }
}
