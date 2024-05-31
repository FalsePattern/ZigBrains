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

package com.falsepattern.zigbrains.zig.lsp;

import com.falsepattern.zigbrains.ZigBundle;
import com.falsepattern.zigbrains.common.util.ApplicationUtil;
import com.falsepattern.zigbrains.common.util.StringUtil;
import com.falsepattern.zigbrains.lsp.IntellijLanguageClient;
import com.falsepattern.zigbrains.lsp.utils.FileUtils;
import com.falsepattern.zigbrains.zig.environment.ZLSConfigProvider;
import com.falsepattern.zigbrains.zig.settings.ZLSProjectSettingsService;
import com.google.gson.Gson;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.PluginManagerConfigurable;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.util.io.FileUtil;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class ZLSStartupActivity implements ProjectActivity {
    private static final Logger LOG = Logger.getInstance(ZLSStartupActivity.class);
    private static final ReentrantLock lock = new ReentrantLock();

    public static void initZLS(Project project) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            lock.lock();
            try {
                var wrappers = IntellijLanguageClient.getAllServerWrappersFor(FileUtils.projectToUri(project));
                for (var wrapper : wrappers) {
                    if (wrapper.serverDefinition.ext.equals("zig")) {
                        wrapper.stop(false);
                        IntellijLanguageClient.removeWrapper(wrapper);
                    }
                }
                var svc = ZLSProjectSettingsService.getInstance(project);
                val state = svc.getState();
                var zlsPath = state.zlsPath;
                if (!validatePath("ZLS Binary", zlsPath, false)) {
                    return;
                }
                var configPath = state.zlsConfigPath;
                boolean configOK = true;
                if (!configPath.isBlank() && !validatePath("ZLS Config", configPath, false)) {
                    Notifications.Bus.notify(new Notification("ZigBrains.ZLS", "Using default config path.",
                                                              NotificationType.INFORMATION));
                    configPath = null;
                }
                if (configPath == null || configPath.isBlank()) {
                    blk:
                    try {
                        val tmpFile = FileUtil.createTempFile("zigbrains-zls-autoconf", ".json", true).toPath();
                        val config = ZLSConfigProvider.findEnvironment(project);
                        if (StringUtil.isEmpty(config.zig_exe_path()) && StringUtil.isEmpty(config.zig_lib_path())) {
                            // TODO this generates unnecessary noise in non-zig projects, find an alternative.
                            // Notifications.Bus.notify(new Notification("ZigBrains.ZLS", "(ZLS) Failed to detect zig path from project toolchain", NotificationType.WARNING));
                            configOK = false;
                            break blk;
                        }
                        try (val writer = Files.newBufferedWriter(tmpFile)) {
                            val gson = new Gson();
                            gson.toJson(config, writer);
                        }
                        configPath = tmpFile.toAbsolutePath().toString();
                    } catch (IOException e) {
                        Notifications.Bus.notify(new Notification("ZigBrains.ZLS", "Failed to create automatic zls config file",
                                                                  NotificationType.WARNING));
                        LOG.warn(e);
                        configOK = false;
                    }
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
                if (state.increaseTimeouts) {
                    for (var timeout : IntellijLanguageClient.getTimeouts().keySet()) {
                        IntellijLanguageClient.setTimeout(timeout, 15000);
                    }
                }

                if (state.debug) {
                    cmd.add("--enable-debug-log");
                }
                if (state.messageTrace) {
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
        if (pathTxt == null || pathTxt.isBlank()) {
            return false;
        }
        Path path;
        try {
            path = Path.of(pathTxt);
        } catch (InvalidPathException e) {
            Notifications.Bus.notify(
                    new Notification("ZigBrains.ZLS", "No " + name, "Invalid " + name + " at path \"" + pathTxt + "\"",
                                     NotificationType.ERROR));
            return false;
        }
        if (!Files.exists(path)) {
            Notifications.Bus.notify(new Notification("ZigBrains.ZLS", "No " + name,
                                                      "The " + name + " at \"" + pathTxt + "\" doesn't exist!",
                                                      NotificationType.ERROR));
            return false;
        }
        if (Files.isDirectory(path) != dir) {
            Notifications.Bus.notify(new Notification("ZigBrains.ZLS", "No " + name,
                                                      "The " + name + " at \"" + pathTxt + "\" is a " +
                                                      (Files.isDirectory(path) ? "directory" : "file") +
                                                      ", expected a " + (dir ? "directory" : "file"),
                                                      NotificationType.ERROR));
            return false;
        }
        return true;
    }

    private static boolean firstInit = true;

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        val svc = ZLSProjectSettingsService.getInstance(project);
        val state = svc.getState();
        if (firstInit) {
            firstInit = false;
            if (!PluginManager.isPluginInstalled(PluginId.getId("com.intellij.modules.cidr.debugger")) && PluginManager.isPluginInstalled(PluginId.getId("com.intellij.modules.nativeDebug-plugin-capable"))) {
                val notif = new Notification(
                        "ZigBrains",
                        ZigBundle.message("notification.nativedebug.title"),
                        ZigBundle.message("notification.nativedebug.text"),
                        NotificationType.INFORMATION
                );
                notif.addAction(new NotificationAction(ZigBundle.message("notification.nativedebug.market")) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                        val configurable = new PluginManagerConfigurable();
                        ShowSettingsUtil.getInstance().editConfigurable((Project)null,
                                                                        configurable,
                                                                        () -> {
                                                                            configurable.openMarketplaceTab("/vendor:\"JetBrains s.r.o.\" /tag:Debugging \"Native Debugging Support\"");
                                                                        });
                    }
                });
                notif.addAction(new NotificationAction(ZigBundle.message("notification.nativedebug.browser")) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                        BrowserUtil.browse("https://plugins.jetbrains.com/plugin/12775-native-debugging-support");
                    }
                });
                Notifications.Bus.notify(notif);
            }
        }
        var zlsPath = state.zlsPath;

        if (zlsPath == null) {
            //Project creation
            ApplicationUtil.pool(() -> {
                initZLS(project);
            }, 5, TimeUnit.SECONDS);
            return null;
        }

        if (zlsPath.isEmpty()) {
            Notifications.Bus.notify(new Notification("ZigBrains.ZLS", "No ZLS binary",
                                                      "Please configure the path to the zls executable in the Zig language configuration menu!",
                                                      NotificationType.INFORMATION));
            return null;
        }
        initZLS(project);
        return null;
    }
}
