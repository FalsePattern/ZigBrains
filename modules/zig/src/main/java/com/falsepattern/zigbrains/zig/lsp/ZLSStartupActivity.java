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
import com.falsepattern.zigbrains.zig.settings.ZLSProjectSettingsService;
import com.falsepattern.zigbrains.zig.util.JBInternalPluginManagerConfigurable;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.redhat.devtools.lsp4ij.LanguageServerManager;
import com.redhat.devtools.lsp4ij.ServerStatus;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

public class ZLSStartupActivity implements ProjectActivity {
    public static void startLSP(Project project, boolean restart) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            val manager = LanguageServerManager.getInstance(project);
            val status = manager.getServerStatus("ZigBrains");
            if ((status == ServerStatus.started || status == ServerStatus.starting) && !restart)
                return;
            LanguageServerManager.getInstance(project).start("ZigBrains");
        });
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
                if (JBInternalPluginManagerConfigurable.successful) {
                    notif.addAction(new NotificationAction(ZigBundle.message("notification.nativedebug.market")) {
                        @Override
                        public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                            val configurable = new JBInternalPluginManagerConfigurable();
                            ShowSettingsUtil.getInstance().editConfigurable((Project) null, configurable.instance, () -> {
                                configurable.openMarketplaceTab("/vendor:\"JetBrains s.r.o.\" /tag:Debugging \"Native Debugging Support\"");
                            });
                        }
                    });
                }
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
                startLSP(project, false);
            }, 5, TimeUnit.SECONDS);
            return null;
        }

        if (zlsPath.isEmpty()) {
            Notifications.Bus.notify(new Notification("ZigBrains.ZLS", "No ZLS binary",
                                                      "Please configure the path to the zls executable in the Zig language configuration menu!",
                                                      NotificationType.INFORMATION));
            return null;
        }
        startLSP(project, false);
        return null;
    }
}
