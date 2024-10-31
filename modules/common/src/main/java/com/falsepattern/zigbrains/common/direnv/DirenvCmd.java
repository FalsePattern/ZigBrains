package com.falsepattern.zigbrains.common.direnv;

import com.falsepattern.zigbrains.common.util.FileUtil;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.Key;
import com.intellij.util.concurrency.AppExecutorUtil;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class DirenvCmd {
    private static final String GROUP_DISPLAY_ID = "ZigBrains Direnv";
    private static final Logger LOG = Logger.getInstance(DirenvCmd.class);

    public static final Key<Boolean> DIRENV_KEY = Key.create("ZIG_DIRENV_KEY");

    private final Path workDir;

    public DirenvCmd(Path workingDirectory) {
        this.workDir = workingDirectory;
    }

    public static boolean direnvInstalled() {
        return FileUtil.findExecutableOnPATH(Map.of(), "direnv").isPresent();
    }

    public @NotNull Map<String, String> importDirenvSync() {
        val emptyMap = Map.<String, String>of();
        if (!direnvInstalled()) {
            return emptyMap;
        }
        try {
            try {
                val runOutput = runSync("export", "json");
                if (runOutput.error()) {
                    if (runOutput.output().contains("is blocked")) {
                        Notifications.Bus.notify(new Notification(GROUP_DISPLAY_ID, "Direnv not allowed",
                                                                  "Run `direnv allow` in a terminal inside the project directory" +
                                                                  " to allow direnv to run", NotificationType.ERROR));
                    } else {
                        Notifications.Bus.notify(new Notification(GROUP_DISPLAY_ID, "Direnv error",
                                                                  "Could not import direnv: " + runOutput.output(),
                                                                  NotificationType.ERROR));
                        return emptyMap;
                    }
                }

                val type = new TypeToken<Map<String, String>>() {
                }.getType();
                if (runOutput.output().isEmpty()) {
                    return emptyMap;
                }

                return new Gson().fromJson(runOutput.output(), type);
            } catch (Exception e) {
                LOG.error("Failed to import direnv", e);
                return emptyMap;
            }
        } catch (Exception e) {
            LOG.error("Failed to import direnv", e);
            return emptyMap;
        }
    }

    public static @NotNull CompletableFuture<Map<String, String>> tryGetProjectEnvAsync(@Nullable Project project) {
        if (project == null)
            return CompletableFuture.completedFuture(Map.of());
        val dir = ProjectUtil.guessProjectDir(project);
        if (dir == null) {
            return CompletableFuture.completedFuture(Map.of());
        }
        val direnv = new DirenvCmd(dir.toNioPath());
        return direnv.importDirenvAsync();
    }

    public static @NotNull Map<String, String> tryGetProjectEnvSync(@Nullable Project project) {
        if (project == null)
            return Map.of();
        val dir = ProjectUtil.guessProjectDir(project);
        if (dir == null) {
            return Map.of();
        }
        val direnv = new DirenvCmd(dir.toNioPath());
        return direnv.importDirenvSync();
    }

    public @NotNull CompletableFuture<Map<String, String>> importDirenvAsync() {
        val emptyMap = Map.<String, String>of();
        var returnMap = CompletableFuture.completedFuture(emptyMap);
        if (!direnvInstalled()) {
            return returnMap;
        }
        return runAsync("export", "json").thenApplyAsync(runOutput -> {
            if (runOutput.error()) {
                if (runOutput.output().contains("is blocked")) {
                    Notifications.Bus.notify(new Notification(GROUP_DISPLAY_ID, "Direnv not allowed",
                                                              "Run `direnv allow` in a terminal inside the project directory" +
                                                              " to allow direnv to run", NotificationType.ERROR));
                } else {
                    Notifications.Bus.notify(new Notification(GROUP_DISPLAY_ID, "Direnv error",
                                                              "Could not import direnv: " + runOutput.output(),
                                                              NotificationType.ERROR));
                    return emptyMap;
                }
            }

            val type = new TypeToken<Map<String, String>>() {
            }.getType();
            if (runOutput.output().isEmpty()) {
                return emptyMap;
            }

            return new Gson().fromJson(runOutput.output(), type);
        }, AppExecutorUtil.getAppExecutorService()).exceptionallyAsync((e) -> {
            LOG.error("Failed to import direnv", e);
            return emptyMap;
        }, AppExecutorUtil.getAppExecutorService());
    }

    private CompletableFuture<DirenvOutput> runAsync(String... args) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return runSync(args);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, AppExecutorUtil.getAppExecutorService());
    }

    private DirenvOutput runSync(String... args) throws ExecutionException, InterruptedException, IOException {
        val commandArgs = new String[args.length + 1];
        commandArgs[0] = "direnv";
        System.arraycopy(args, 0, commandArgs, 1, args.length);
        val cli = new GeneralCommandLine(commandArgs).withWorkDirectory(workDir.toFile());
        val process = cli.createProcess();

        if (process.waitFor() != 0) {
            val stdErr = IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8);
            return new DirenvOutput(stdErr, true);
        }

        val stdOut = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);

        return new DirenvOutput(stdOut, false);
    }
}
