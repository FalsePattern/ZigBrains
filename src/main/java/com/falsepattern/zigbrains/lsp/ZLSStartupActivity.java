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

package com.falsepattern.zigbrains.lsp;

import com.falsepattern.zigbrains.settings.AppSettingsState;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.IntellijLanguageClient;
import org.wso2.lsp4intellij.client.languageserver.serverdefinition.RawCommandServerDefinition;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;

public class ZLSStartupActivity implements StartupActivity {
    public static void initZLS() {
        var settings = AppSettingsState.getInstance();
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
        if (settings.debug) {
            cmd.add("--enable-debug-log");
        }
        if (settings.messageTrace) {
            cmd.add("--enable-message-tracing");
        }
        IntellijLanguageClient.addServerDefinition(new RawCommandServerDefinition("zig", cmd.toArray(String[]::new)));
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
                                                      "The " + name + " at \"" + pathTxt +
                                                      "\" doesn't exist!", NotificationType.ERROR));
            return false;
        }
        if (Files.isDirectory(path) != dir) {
            Notifications.Bus.notify(new Notification("ZigBrains.Nag", "No " + name,
                                                      "The " + name + " at \"" + pathTxt +
                                                      "\" is a " + (Files.isDirectory(path) ? "directory": "file") +
                                                      " , expected a " + (dir ? "directory" : "file"), NotificationType.ERROR));
            return false;
        }
        return true;
    }

    @Override
    public void runActivity(@NotNull Project project) {
        var path = AppSettingsState.getInstance().zlsPath;
        if ("".equals(path)) {
            Notifications.Bus.notify(new Notification("ZigBrains.Nag", "No ZLS binary",
                                                      "Please configure the path to the zls executable in the Zig language configuration menu!",
                                                      NotificationType.INFORMATION));
            return;
        }
        initZLS();
    }
}
