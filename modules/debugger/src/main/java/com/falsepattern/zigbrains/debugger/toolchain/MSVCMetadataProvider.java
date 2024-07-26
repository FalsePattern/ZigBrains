package com.falsepattern.zigbrains.debugger.toolchain;

import com.falsepattern.zigbrains.ZigBundle;
import com.falsepattern.zigbrains.debugger.settings.MSVCDownloadPermission;
import com.falsepattern.zigbrains.debugger.settings.ZigDebuggerSettings;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.download.DownloadableFileService;
import lombok.Cleanup;
import lombok.val;

import javax.swing.BoxLayout;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static com.falsepattern.zigbrains.debugger.toolchain.ZigDebuggerToolchainService.downloadPath;

class MSVCMetadataProvider {
    private Properties cached;
    private Future<Properties> downloadMSVCProps() {
        val future = new CompletableFuture<Properties>();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                val service = DownloadableFileService.getInstance();
                val desc = service.createFileDescription("https://falsepattern.com/zigbrains/msvc.properties", "msvc.properties");
                val downloader = service.createDownloader(List.of(desc), "Debugger metadata downloading");
                val downloadDirectory = downloadPath().toFile();
                val prop = new Properties();
                val downloadResults = downloader.download(downloadDirectory);
                for (val result : downloadResults) {
                    if (Objects.equals(result.second.getDefaultFileName(), "msvc.properties")) {
                        @Cleanup val reader = new FileReader(result.first);
                        prop.load(reader);
                    }
                }
                future.complete(prop);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private Properties fetchBuiltinMSVCProps() {
        val prop = new Properties();
        try {
            @Cleanup val resource = ZigDebuggerToolchainService.class.getResourceAsStream("/msvc.properties");
            if (resource == null) {
                Notifications.Bus.notify(new Notification(
                        "ZigBrains.Debugger.Error",
                        ZigBundle.message("notification.title.debugger"),
                        ZigBundle.message("notification.content.debugger.metadata.fallback.fetch.failed"),
                        NotificationType.ERROR
                ));
                return prop;
            }
            val reader = new InputStreamReader(resource);
            prop.load(reader);
        } catch (IOException ex) {
            ex.printStackTrace();
            Notifications.Bus.notify(new Notification(
                    "ZigBrains.Debugger.Error",
                    ZigBundle.message("notification.title.debugger"),
                    ZigBundle.message("notification.content.debugger.metadata.fallback.parse.failed"),
                    NotificationType.ERROR
            ));
        }
        return prop;
    }


    Properties msvcProperties() {
        if (cached != null)
            return cached;
        val settings = ZigDebuggerSettings.getInstance();
        var permission = settings.msvcConsent;
        if (permission == MSVCDownloadPermission.AskMe) {
            AtomicReference<Boolean> accepted = new AtomicReference<>(false);
            ApplicationManager.getApplication().invokeAndWait(() -> {
                val dialog = new DialogBuilder();
                dialog.setTitle("Network Request Consent");
                dialog.addCancelAction().setText("Deny");
                dialog.addOkAction().setText("Allow");
                val centerPanel = new JBPanel<>();
                centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
                centerPanel.add(new JBLabel("ZigBrains needs to download some metadata from the internet for debugging on Windows."));
                centerPanel.add(new JBLabel("ZigBrains will use the fallback metadata shipped inside the plugin if the request is denied."));
                centerPanel.add(new JBLabel("Would you like to allow this network request?"));
                dialog.centerPanel(centerPanel);
                accepted.set(dialog.showAndGet());
            });
            permission = settings.msvcConsent = accepted.get() ? MSVCDownloadPermission.Allow : MSVCDownloadPermission.Deny;
        }
        return switch (permission) {
            //noinspection DataFlowIssue
            case AskMe, Deny -> cached = fetchBuiltinMSVCProps();
            case Allow -> {
                val future = downloadMSVCProps();
                try {
                    yield future.get(3, TimeUnit.SECONDS);
                } catch (ExecutionException | InterruptedException | TimeoutException e) {
                    e.printStackTrace();
                    Notifications.Bus.notify(new Notification(
                            "ZigBrains.Debugger.Error",
                            ZigBundle.message("notification.title.debugger"),
                            ZigBundle.message("notification.content.debugger.metadata.downloading.failed"),
                            NotificationType.ERROR
                    ));
                    settings.msvcConsent = MSVCDownloadPermission.Deny;
                    yield cached = fetchBuiltinMSVCProps();
                }
            }
        };
    }
}
