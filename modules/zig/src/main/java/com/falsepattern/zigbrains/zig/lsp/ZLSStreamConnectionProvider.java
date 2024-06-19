package com.falsepattern.zigbrains.zig.lsp;

import com.falsepattern.zigbrains.common.util.StringUtil;
import com.falsepattern.zigbrains.zig.environment.ZLSConfigProvider;
import com.falsepattern.zigbrains.zig.settings.ZLSProjectSettingsService;
import com.google.gson.Gson;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.redhat.devtools.lsp4ij.server.ProcessStreamConnectionProvider;
import lombok.val;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ZLSStreamConnectionProvider extends ProcessStreamConnectionProvider {
    private static final Logger LOG = Logger.getInstance(ZLSStreamConnectionProvider.class);
    public ZLSStreamConnectionProvider(Project project) {
        super.setCommands(getCommand(project));
    }

    public static List<String> getCommand(Project project) {
        var svc = ZLSProjectSettingsService.getInstance(project);
        val state = svc.getState();
        var zlsPath = state.zlsPath;
        if (!validatePath("ZLS Binary", zlsPath, false)) {
            return null;
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

        var cmd = new ArrayList<String>();
        cmd.add(zlsPath);
        if (configOK) {
            cmd.add("--config-path");
            cmd.add(configPath);
        }

        if (state.debug) {
            cmd.add("--enable-debug-log");
        }
        if (state.messageTrace) {
            cmd.add("--enable-message-tracing");
        }
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            for (int i = 0; i < cmd.size(); i++) {
                if (cmd.get(i).contains(" ")) {
                    cmd.set(i, '"' + cmd.get(i) + '"');
                }
            }
        }
        return cmd;
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
}
