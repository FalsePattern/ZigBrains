package com.falsepattern.zigbrains.zig.lsp;

import com.falsepattern.zigbrains.common.util.StringUtil;
import com.falsepattern.zigbrains.zig.environment.ZLSConfigProvider;
import com.falsepattern.zigbrains.zig.settings.ZLSProjectSettingsService;
import com.google.gson.Gson;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.lsp4ij.server.ProcessStreamConnectionProvider;
import lombok.val;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class ZLSStreamConnectionProvider extends ProcessStreamConnectionProvider {
    private static final Logger LOG = Logger.getInstance(ZLSStreamConnectionProvider.class);
    public ZLSStreamConnectionProvider(Project project) {
        val command = getCommand(project);
        val projectDir = ProjectUtil.guessProjectDir(project);
        if (projectDir != null) {
            setWorkingDirectory(projectDir.getPath());
        }
        try {
            setCommands(command.get());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<String> doGetCommand(Project project) {
        var svc = ZLSProjectSettingsService.getInstance(project);
        val state = svc.getState();
        var zlsPath = state.zlsPath;
        if (StringUtil.isEmpty(zlsPath)) {
            zlsPath = com.falsepattern.zigbrains.common.util.FileUtil.findExecutableOnPATH("zls").map(Path::toString).orElse(null);
            if (zlsPath == null) {
                Notifications.Bus.notify(new Notification("ZigBrains.ZLS", "Could not detect ZLS binary! Please configure it!",
                                                          NotificationType.ERROR));
                return null;
            }
            state.setZlsPath(zlsPath);
        }
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

    public static Future<List<String>> getCommand(Project project) {
        val future = new CompletableFuture<List<String>>();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                future.complete(doGetCommand(project));
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
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
