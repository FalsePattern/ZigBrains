package com.falsepattern.zigbrains.zig.lsp;

import com.falsepattern.zigbrains.common.direnv.DirenvCmd;
import com.falsepattern.zigbrains.common.util.StringUtil;
import com.falsepattern.zigbrains.zig.environment.ZLSConfigProvider;
import com.falsepattern.zigbrains.zig.settings.ZLSProjectSettingsService;
import com.google.gson.Gson;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.redhat.devtools.lsp4ij.server.OSProcessStreamConnectionProvider;
import lombok.val;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.jsonrpc.messages.Message;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseMessage;
import org.eclipse.lsp4j.services.LanguageServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

public class ZLSStreamConnectionProvider extends OSProcessStreamConnectionProvider {
    private static final Logger LOG = Logger.getInstance(ZLSStreamConnectionProvider.class);
    private final Project project;
    public ZLSStreamConnectionProvider(Project project) {
        this.project = project;
        val command = getCommandAsync(project, true);
        val projectDir = ProjectUtil.guessProjectDir(project);
        GeneralCommandLine commandLine = null;
        try {
            val cmd = command.get();
            if (cmd != null) {
                commandLine = new GeneralCommandLine(command.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        if (commandLine != null && projectDir != null) {
            commandLine.setWorkDirectory(projectDir.getPath());
        }
        setCommandLine(commandLine);
    }

    @Override
    public void handleMessage(Message message, LanguageServer languageServer, VirtualFile rootUri) {
        if (ZLSProjectSettingsService.getInstance(project).getState().inlayHintsCompact) {
            if (message instanceof ResponseMessage resp) {
                val res = resp.getResult();
                if (res instanceof Collection<?> c) {
                    c.forEach(e -> {
                        if (e instanceof InlayHint ih) {
                            tryMutateInlayHint(ih);
                        }
                    });
                } else if (res instanceof InlayHint ih) {
                    tryMutateInlayHint(ih);
                }
            }
        }
        super.handleMessage(message, languageServer, rootUri);
    }

    private static final Pattern ERROR_BLOCK = Pattern.compile("error\\{.*?}", Pattern.DOTALL);


    private void tryMutateInlayHint(InlayHint inlayHint) {
        if (inlayHint.getLabel().isLeft()) {
            val str = inlayHint.getLabel().getLeft();
            val shortened = ERROR_BLOCK.matcher(str).replaceAll("error{...}");
            inlayHint.setLabel(shortened);
        }
    }

    private static @Nullable Path tryWorkDirFromProject(Project project) {
        val projectDir = ProjectUtil.guessProjectDir(project);
        if (projectDir == null) {
            return null;
        }
        return projectDir.toNioPath();
    }

    private static @NotNull CompletableFuture<Map<String, String>> getDirenv(Project project) {
        val workDir = tryWorkDirFromProject(project);
        if (workDir == null)
            return CompletableFuture.completedFuture(Map.of());
        val direnvCmd = new DirenvCmd(workDir);
        //Async if in dispatch thread, sync otherwise
        return ApplicationManager.getApplication().isDispatchThread() ? direnvCmd.importDirenvAsync() : CompletableFuture.completedFuture(direnvCmd.importDirenvSync());
    }

    public static List<String> getCommandSync(Project project, boolean full) {
        var svc = ZLSProjectSettingsService.getInstance(project);
        val state = svc.getState();
        var zlsPath = state.zlsPath;
        if (StringUtil.isEmpty(zlsPath)) {
            Map<String, String> direnv = Map.of();
            if (state.direnv && DirenvCmd.direnvInstalled()) {
                try {
                    direnv = getDirenv(project).get();
                } catch (InterruptedException | ExecutionException e) {
                    LOG.warn(e);
                    direnv = Map.of();
                }
            }
            zlsPath = com.falsepattern.zigbrains.common.util.FileUtil.findExecutableOnPATH(direnv, "zls").map(Path::toString).orElse(null);
            if (zlsPath == null) {
                if (full) {
                    Notifications.Bus.notify(
                            new Notification("ZigBrains.ZLS", "Could not detect ZLS binary! Please configure it!",
                                             NotificationType.ERROR));
                }
                return null;
            }
            val zlsPathFinal = zlsPath;
            ApplicationManager.getApplication().invokeLater(() -> state.setZlsPath(zlsPathFinal));
        }
        if (!validatePath("ZLS Binary", zlsPath, false, full)) {
            return null;
        }
        var configPath = state.zlsConfigPath;
        boolean configOK = true;
        if (!configPath.isBlank() && !validatePath("ZLS Config", configPath, false, full)) {
            if (full) {
                Notifications.Bus.notify(
                        new Notification("ZigBrains.ZLS", "Using default config path.", NotificationType.INFORMATION));
            }
            configPath = null;
        }
        if ((configPath == null || configPath.isBlank()) && full) {
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
                if (full) {
                    Notifications.Bus.notify(
                            new Notification("ZigBrains.ZLS", "Failed to create automatic zls config file",
                                             NotificationType.WARNING));
                }
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

    public static CompletableFuture<List<String>> getCommandAsync(Project project, boolean full) {
        return CompletableFuture.supplyAsync(() -> getCommandSync(project, full), AppExecutorUtil.getAppExecutorService());
    }

    private static boolean validatePath(String name, String pathTxt, boolean dir, boolean full) {
        if (pathTxt == null || pathTxt.isBlank()) {
            return false;
        }
        Path path;
        try {
            path = Path.of(pathTxt);
        } catch (InvalidPathException e) {
            if (full) {
                Notifications.Bus.notify(new Notification("ZigBrains.ZLS", "No " + name,
                                                          "Invalid " + name + " at path \"" + pathTxt + "\"",
                                                          NotificationType.ERROR));
            }
            return false;
        }
        if (!Files.exists(path)) {
            if (full) {
                Notifications.Bus.notify(new Notification("ZigBrains.ZLS", "No " + name,
                                                          "The " + name + " at \"" + pathTxt + "\" doesn't exist!",
                                                          NotificationType.ERROR));
            }
            return false;
        }
        if (Files.isDirectory(path) != dir) {
            if (full) {
                Notifications.Bus.notify(new Notification("ZigBrains.ZLS", "No " + name,
                                                          "The " + name + " at \"" + pathTxt + "\" is a " +
                                                          (Files.isDirectory(path) ? "directory" : "file") +
                                                          ", expected a " + (dir ? "directory" : "file"),
                                                          NotificationType.ERROR));
            }
            return false;
        }
        return true;
    }
}
