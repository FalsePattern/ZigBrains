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

import com.falsepattern.zigbrains.project.ide.project.ZigDefaultTemplate;
import com.falsepattern.zigbrains.project.ide.project.ZigProjectTemplate;
import com.falsepattern.zigbrains.project.openapi.components.ZigProjectSettings;
import com.falsepattern.zigbrains.project.openapi.components.ZigProjectSettingsService;
import com.falsepattern.zigbrains.zig.settings.ZLSProjectSettingsService;
import com.falsepattern.zigbrains.zig.settings.ZLSSettings;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.GitRepositoryInitializer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ResourceUtil;
import lombok.Cleanup;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public record ZigProjectConfigurationData(boolean git, ZigProjectSettings projConf, ZLSSettings zlsConf, ZigProjectTemplate selectedTemplate) {

    private static String getResourceString(String path) throws IOException {
        byte[] data = ResourceUtil.getResourceAsBytes(path, ZigDirectoryProjectGenerator.class.getClassLoader());
        if (data == null)
            throw new IOException("Could not find resource " + path + "!");
        return new String(data, StandardCharsets.UTF_8);
    }

    public void generateProject(@NotNull Object requestor, @NotNull Project project, @NotNull VirtualFile baseDir, boolean forceGitignore) {
        val svc = ZigProjectSettingsService.getInstance(project);
        svc.loadState(this.projConf());
        ZLSProjectSettingsService.getInstance(project).loadState(this.zlsConf());

        val toolchain = svc.getState().getToolchain();

        val template = this.selectedTemplate();

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
                            parentDir = baseDir.createChildDirectory(requestor, fileName.substring(0, slashIndex));
                            fileName = fileName.substring(slashIndex + 1);
                        } else {
                            parentDir = baseDir;
                        }
                        val templateDir = fileTemplate.getValue();
                        val resourceData = getResourceString("project-gen/" + templateDir + "/" + fileName + ".template").replace("@@PROJECT_NAME@@", projectName);
                        val targetFile = parentDir.createChildData(requestor, fileName);
                        VfsUtil.saveText(targetFile, resourceData);
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (git) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                val initializer = GitRepositoryInitializer.getInstance();
                if (initializer != null) {
                    initializer.initRepository(project, baseDir);
                }
            });
        }

        if (git || forceGitignore) {
            createGitIgnoreFile(baseDir, this);
        }
    }


    private static final String GITIGNORE = ".gitignore";

    private static void createGitIgnoreFile(VirtualFile projectDir, Object requestor) {
        val existingFile = projectDir.findChild(GITIGNORE);
        if (existingFile != null) {
            return;
        }
        WriteAction.run(() -> {
            VirtualFile file = null;
            try {
                file = projectDir.createChildData(requestor, GITIGNORE);
                @Cleanup val res = ZigProjectConfigurationData.class.getResourceAsStream("/fileTemplates/internal/gitignore");
                if (res == null)
                    return;
                file.setCharset(StandardCharsets.UTF_8);
                file.setBinaryContent(res.readAllBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
