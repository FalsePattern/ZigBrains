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

package com.falsepattern.zigbrains.zig.lsp;

import com.falsepattern.zigbrains.lsp.utils.FileUtils;
import com.falsepattern.zigbrains.zig.settings.ZLSSettingsState;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.falsepattern.zigbrains.lsp.IntellijLanguageClient;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class ZLSStartupActivity implements ProjectActivity {
    private static final ReentrantLock lock = new ReentrantLock();

    public static void initZLS(Project project) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            lock.lock();
            try {
                var wrappers = IntellijLanguageClient.getAllServerWrappersFor(FileUtils.projectToUri(project));
                for (var wrapper : wrappers) {
                    if (wrapper.serverDefinition.ext.equals("zig")) {
                        wrapper.stop(false);
                        wrapper.removeWidget();
                        IntellijLanguageClient.removeWrapper(wrapper);
                    }
                }
                var settings = ZLSSettingsState.getInstance(project);
                var zlsPath = settings.zlsPath;
                if (!validatePath("ZLS Binary", zlsPath, false)) {
                    return;
                }
                var configPath = settings.zlsConfigPath;
                boolean configOK = true;
                if (!"".equals(configPath) && !validatePath("ZLS Config", configPath, false)) {
                    configOK = false;
                    Notifications.Bus.notify(new Notification("ZigBrains.Nag", "Using default config path.",
                                                              NotificationType.INFORMATION));
                }
                if ("".equals(configPath)) {
                    configOK = false;
                }

                if (IntellijLanguageClient.getExtensionManagerFor("zig") == null) {
                    IntellijLanguageClient.addExtensionManager("zig", new ZLSExtensionManager());
                }
                var cmd = new ArrayList<String>();
                cmd.add(zlsPath);
                if (configOK) {
                    cmd.add("--config-path");
                    cmd.add(configPath);
                }
                // TODO make this properly configurable
                if (settings.increaseTimeouts) {
                    for (var timeout : IntellijLanguageClient.getTimeouts().keySet()) {
                        IntellijLanguageClient.setTimeout(timeout, 15000);
                    }
                }

                if (settings.debug) {
                    cmd.add("--enable-debug-log");
                }
                if (settings.messageTrace) {
                    cmd.add("--enable-message-tracing");
                }
                for (var wrapper : IntellijLanguageClient.getAllServerWrappersFor("zig")) {
                    wrapper.removeServerWrapper();
                }
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    for (int i = 0; i < cmd.size(); i++) {
                        if (cmd.get(i).contains(" ")) {
                            cmd.set(i, '"' + cmd.get(i) + '"');
                        }
                    }
                }
                IntellijLanguageClient.addServerDefinition(new ZLSServerDefinition(cmd.toArray(String[]::new)), project);
            } finally {
                lock.unlock();
            }
        });
    }

    private static boolean validatePath(String name, String pathTxt, boolean dir) {
        Path path;
        try {
            path = Path.of(pathTxt);
        } catch (InvalidPathException e) {
            Notifications.Bus.notify(
                    new Notification("ZigBrains.Nag", "No " + name, "Invalid " + name + " path \"" + pathTxt + "\"",
                                     NotificationType.ERROR));
            return false;
        }
        if (!Files.exists(path)) {
            Notifications.Bus.notify(new Notification("ZigBrains.Nag", "No " + name,
                                                      "The " + name + " at \"" + pathTxt + "\" doesn't exist!",
                                                      NotificationType.ERROR));
            return false;
        }
        if (Files.isDirectory(path) != dir) {
            Notifications.Bus.notify(new Notification("ZigBrains.Nag", "No " + name,
                                                      "The " + name + " at \"" + pathTxt + "\" is a " +
                                                      (Files.isDirectory(path) ? "directory" : "file") +
                                                      " , expected a " + (dir ? "directory" : "file"),
                                                      NotificationType.ERROR));
            return false;
        }
        return true;
    }

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        var path = ZLSSettingsState.getInstance(project).zlsPath;
        if ("".equals(path)) {
            Notifications.Bus.notify(new Notification("ZigBrains.Nag", "No ZLS binary",
                                                      "Please configure the path to the zls executable in the Zig language configuration menu!",
                                                      NotificationType.INFORMATION));
            return null;
        }
        initZLS(project);
        return null;
    }
}
