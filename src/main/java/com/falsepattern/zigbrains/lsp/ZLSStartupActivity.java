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
import org.wso2.lsp4intellij.client.languageserver.serverdefinition.ProcessBuilderServerDefinition;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public class ZLSStartupActivity implements StartupActivity {
    public static void initZLS() {
        var pathTxt = AppSettingsState.getInstance().zlsPath;
        Path path;
        try {
            path = Path.of(pathTxt);
        } catch (InvalidPathException e) {
            Notifications.Bus.notify(new Notification("ZigBrains.Nag",
                                                      "No ZLS binary",
                                                      "Invalid ZLS binary path \"" + pathTxt + "\"",
                                                      NotificationType.ERROR));
            return;
        }
        if (!Files.exists(path) || Files.isDirectory(path)) {
            Notifications.Bus.notify(new Notification("ZigBrains.Nag",
                                                      "No ZLS binary",
                                                      "The ZLS binary at \"" + pathTxt + "\" doesn't exist or is a directory!",
                                                      NotificationType.ERROR));
        }
        if (IntellijLanguageClient.getExtensionManagerFor("zig") == null) {
            IntellijLanguageClient.addExtensionManager("zig", new ZLSExtensionManager());
        }
        var procBuilder = new ProcessBuilder();
        procBuilder.command(pathTxt, "--enable-message-tracing");
        IntellijLanguageClient.addServerDefinition(new ProcessBuilderServerDefinition("zig", procBuilder));
    }

    @Override
    public void runActivity(@NotNull Project project) {
        var path = AppSettingsState.getInstance().zlsPath;
        if ("".equals(path)) {
            Notifications.Bus.notify(new Notification("ZigBrains.Nag",
                                                      "No ZLS binary",
                                                      "Please configure the path to the zls executable in the Zig language configuration menu!",
                                                      NotificationType.INFORMATION));
            return;
        }
        initZLS();
    }
}
