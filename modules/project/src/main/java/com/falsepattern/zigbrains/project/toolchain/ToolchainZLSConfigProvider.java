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

package com.falsepattern.zigbrains.project.toolchain;

import com.falsepattern.zigbrains.project.openapi.components.ZigProjectSettingsService;
import com.falsepattern.zigbrains.zig.environment.ZLSConfig;
import com.falsepattern.zigbrains.zig.environment.ZLSConfigProvider;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import lombok.val;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public class ToolchainZLSConfigProvider implements ZLSConfigProvider {
    @Override
    public void getEnvironment(Project project, ZLSConfig.ZLSConfigBuilder builder) {
        val svc = ZigProjectSettingsService.getInstance(project);
        val state = svc.getState();
        var toolchain = state.getToolchain();
        if (toolchain == null) {
            toolchain = AbstractZigToolchain.suggest();
            if (toolchain == null) {
                return;
            }
            state.setToolchain(toolchain);
        }
        val projectDir = ProjectUtil.guessProjectDir(project);
        val oEnv = toolchain.zig().getEnv(projectDir == null ? null : projectDir.toNioPath());
        if (oEnv.isEmpty()) {
            return;
        }
        val env = oEnv.get();
        Path exe;
        try {
            exe = Path.of(env.zigExecutable());
        } catch (InvalidPathException e) {
            Notifications.Bus.notify(new Notification("ZigBrains.Project",
                                                      "Invalid zig executable path: " + env.zigExecutable(),
                                                      NotificationType.ERROR));
            return;
        }
        if (!exe.isAbsolute()) {
            exe = toolchain.getLocation().resolve(exe);
        }
        if (!Files.exists(exe)) {
            Notifications.Bus.notify(new Notification("ZigBrains.Project",
                                                      "Zig executable path does not exist: " + env.zigExecutable(),
                                                      NotificationType.ERROR));
            return;
        }
        Path lib = null;
        override:
        if (state.overrideStdPath) {
            try {
                lib = Path.of(state.explicitPathToStd);
            } catch (InvalidPathException e) {
                Notifications.Bus.notify(new Notification("ZigBrains.Project",
                                                          "Invalid zig standard library path override: " + env.zigExecutable(),
                                                          NotificationType.ERROR));
                state.overrideStdPath = false;
                break override;
            }
            if (!lib.isAbsolute()) {
                lib = toolchain.getLocation().resolve(lib);
            }
            if (!Files.exists(lib)) {
                Notifications.Bus.notify(new Notification("ZigBrains.Project",
                                                          "Zig standard library path override does not exist: " + env.zigExecutable(),
                                                          NotificationType.ERROR));
                lib = null;
                state.overrideStdPath = false;
                break override;
            } else {
                state.explicitPathToStd = lib.toString();
            }
        }
        if (lib == null) {
            try {
                lib = Path.of(env.libDirectory());
            } catch (InvalidPathException e) {
                Notifications.Bus.notify(new Notification("ZigBrains.Project",
                                                          "Invalid zig standard library path: " + env.zigExecutable(),
                                                          NotificationType.ERROR));
                return;
            }
            if (!lib.isAbsolute()) {
                lib = toolchain.getLocation().resolve(lib);
            }
            if (!Files.exists(lib)) {
                Notifications.Bus.notify(new Notification("ZigBrains.Project",
                                                          "Zig standard library path does not exist: " +
                                                          env.zigExecutable(), NotificationType.ERROR));
                return;
            }
        }
        builder.zig_exe_path(exe.toString());
        builder.zig_lib_path(lib.toString());

    }
}
