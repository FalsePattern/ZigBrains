package com.falsepattern.zigbrains.project.steps.discovery;


import com.falsepattern.zigbrains.ZigBundle;
import com.falsepattern.zigbrains.project.util.ProjectUtil;
import com.intellij.ide.impl.TrustedProjects;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import kotlin.Pair;
import lombok.val;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

@Service(Service.Level.PROJECT)
public final class ZigStepDiscoveryService {
    private static final Logger LOG = Logger.getInstance(ZigStepDiscoveryService.class);
    private static final int DEFAULT_TIMEOUT_SEC = 10;

    private final Project project;

    public ZigStepDiscoveryService(Project project) {
        this.project = project;
    }
    private final AtomicBoolean reloading = new AtomicBoolean(false);
    private final AtomicBoolean reloadScheduled = new AtomicBoolean(false);
    private int CURRENT_TIMEOUT_SEC = DEFAULT_TIMEOUT_SEC;

    private void doReload() {
        val bus = project.getMessageBus().syncPublisher(ZigStepDiscoveryListener.TOPIC);
        bus.preReload();
        try {
            val toolchain = ProjectUtil.getToolchain(project);
            if (toolchain == null) {
                bus.errorReload(ZigStepDiscoveryListener.ErrorType.MissingToolchain);
                return;
            }

            val zig = toolchain.zig();
            val result = zig.callWithArgs(
                    ProjectUtil.guessProjectDir(project), CURRENT_TIMEOUT_SEC * 1000,
                    "build", "-l");
            if (result.isPresent()) {
                val res = result.get();
                if (res.getExitCode() == 0) {
                    if (!res.isTimeout()) {
                        CURRENT_TIMEOUT_SEC = DEFAULT_TIMEOUT_SEC;
                        val lines = res.getStdoutLines();
                        val steps = new ArrayList<Pair<String, String>>();
                        for (val line : lines) {
                            val parts = line.trim().split("\\s+", 2);
                            if (parts.length == 2) {
                                steps.add(new Pair<>(parts[0], parts[1]));
                            } else {
                                steps.add(new Pair<>(parts[0], null));
                            }
                        }
                        bus.postReload(steps);
                    } else {
                        bus.timeoutReload(CURRENT_TIMEOUT_SEC);
                        CURRENT_TIMEOUT_SEC *= 2;
                    }
                } else if (res.getStderrLines()
                              .stream()
                              .anyMatch(line -> line.contains(
                                      "error: no build.zig file found, in the current directory or any parent directories"))) {
                    bus.errorReload(ZigStepDiscoveryListener.ErrorType.MissingBuildZig);
                } else {
                    bus.errorReload(ZigStepDiscoveryListener.ErrorType.GeneralError);
                }
            }
        } catch (Throwable t) {
            LOG.error("Error while reloading zig build steps", t);
        }
        synchronized (reloading) {
            if (reloadScheduled.getAndSet(false)) {
                dispatchReload();
                return;
            }
            reloading.set(false);
        }
    }

    private void dispatchReload() {
        val manager = ApplicationManager.getApplication();
        manager.invokeLater(() -> {
            FileDocumentManager.getInstance().saveAllDocuments();
            manager.executeOnPooledThread(this::doReload);
        });
    }

    public void triggerReload() {
        synchronized (reloading) {
            if (reloading.get()) {
                reloadScheduled.set(true);
                return;
            }
            reloading.set(true);
        }
        dispatchReload();
    }

    public static ZigStepDiscoveryService getInstance(Project project) {
        return project.getService(ZigStepDiscoveryService.class);
    }
}
