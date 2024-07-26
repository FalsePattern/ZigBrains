package com.falsepattern.zigbrains.debugger.toolchain;

import com.falsepattern.zigbrains.ZigBundle;
import com.falsepattern.zigbrains.common.ZigPathManager;
import com.falsepattern.zigbrains.debugger.settings.MSVCDownloadPermission;
import com.falsepattern.zigbrains.debugger.settings.ZigDebuggerSettings;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.ui.BrowserHyperlinkListener;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.download.DownloadableFileService;
import com.intellij.util.io.Decompressor;
import com.intellij.util.system.CpuArch;
import com.intellij.util.system.OS;
import com.jetbrains.cidr.execution.debugger.CidrDebuggerPathManager;
import com.jetbrains.cidr.execution.debugger.backend.bin.UrlProvider;
import com.jetbrains.cidr.execution.debugger.backend.lldb.LLDBDriverConfiguration;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

@Service(Service.Level.APP)
public final class ZigDebuggerToolchainService {
    private MSVCMetadataProvider msvcProvider = new MSVCMetadataProvider();
    public static ZigDebuggerToolchainService getInstance() {
        return ApplicationManager.getApplication().getService(ZigDebuggerToolchainService.class);
    }

    public DebuggerAvailability debuggerAvailability(DebuggerKind kind) {
        return switch (kind) {
            case LLDB -> lldbAvailability();
            case GDB -> gdbAvailability();
            case MSVC -> msvcAvailability();
        };
    }

    public DebuggerAvailability lldbAvailability() {
        if (LLDBDriverConfiguration.hasBundledLLDB())
            return DebuggerAvailability.Bundled;

        final String frameworkPath, frontendPath;
        if (SystemInfo.isMac) {
            frameworkPath = "LLDB.framework";
            frontendPath = "LLDBFrontend";
        } else if (SystemInfo.isUnix) {
            frameworkPath = "lib/liblldb.so";
            frontendPath = "bin/LLDBFrontend";
        } else if (SystemInfo.isWindows) {
            return DebuggerAvailability.Unavailable;
//            frameworkPath = "bin/liblldb.dll";
//            frontendPath = "bin/LLDBFrontend.exe";
        } else {
            return DebuggerAvailability.Unavailable;
        }

        val lldbPath = lldbPath();
        val frameworkFile = lldbPath.resolve(frameworkPath);
        val frontendFile = lldbPath.resolve(frontendPath);
        if (!Files.exists(frameworkFile) || !Files.exists(frontendFile))
            return DebuggerAvailability.NeedToDownload;

        val versions = loadDebuggerVersions(DebuggerKind.LLDB);
        val urls = lldbUrls();
        if (urls == null)
            return DebuggerAvailability.Unavailable;

        val lldbFrameworkVersion = fileNameWithoutExtension(urls.framework().toString());
        val lldbFrontendVersion = fileNameWithoutExtension(urls.frontend().toString());

        if (!Objects.equals(versions.get(LLDB_FRAMEWORK_PROPERTY_NAME), lldbFrameworkVersion) ||
            !Objects.equals(versions.get(LLDB_FRONTEND_PROPERTY_NAME), lldbFrontendVersion))
            return DebuggerAvailability.NeedToUpdate;

        return new DebuggerAvailability.LLDBBinaries(frameworkFile, frontendFile);
    }

    public DebuggerAvailability gdbAvailability() {
        // Even if we have bundled GDB, it still doesn't work on macOS for local runs
        if (SystemInfo.isMac)
            return DebuggerAvailability.Unavailable;

        if (CidrDebuggerPathManager.getBundledGDBBinary().exists())
            return DebuggerAvailability.Bundled;

        final String gdbBinaryPath;
        if (SystemInfo.isUnix) {
            gdbBinaryPath = "bin/gdb";
        } else if (SystemInfo.isWindows) {
            return DebuggerAvailability.Unavailable;
//            gdbBinaryPath = "bin/gdb.exe";
        } else {
            return DebuggerAvailability.Unavailable;
        }

        val gdbFile = gdbPath().resolve(gdbBinaryPath);
        if (!Files.exists(gdbFile))
            return DebuggerAvailability.NeedToDownload;

        val versions = loadDebuggerVersions(DebuggerKind.GDB);
        val gdbUrl = gdbUrl();
        if (gdbUrl == null)
            return DebuggerAvailability.Unavailable;

        val gdbVersion = fileNameWithoutExtension(gdbUrl.toString());

        if (!Objects.equals(versions.get(GDB_PROPERTY_NAME), gdbVersion))
            return DebuggerAvailability.NeedToUpdate;

        return new DebuggerAvailability.GDBBinaries(gdbFile);
    }

    public DebuggerAvailability msvcAvailability() {
        //Only applies to Windows
        if (!SystemInfo.isWindows)
            return DebuggerAvailability.Unavailable;

        val msvcBinaryPath = "vsdbg.exe";

        val msvcFile = msvcPath().resolve(msvcBinaryPath);
        if (!Files.exists(msvcFile))
            return DebuggerAvailability.NeedToDownload;

        val msvcUrl = msvcUrl();
        if (msvcUrl == null) //Fallback in case falsepattern.com goes down
            return new DebuggerAvailability.MSVCBinaries(msvcFile);

        val versions = loadDebuggerVersions(DebuggerKind.MSVC);

        if (!Objects.equals(versions.get(MSVC_PROPERTY_NAME), msvcUrl.version))
            return DebuggerAvailability.NeedToUpdate;

        return new DebuggerAvailability.MSVCBinaries(msvcFile);
    }

    @SneakyThrows
    public DownloadResult downloadDebugger(@Nullable Project project, DebuggerKind debuggerKind) {
        val result = ProgressManager.getInstance()
                                    .runProcessWithProgressSynchronously(() -> downloadDebuggerSynchronously(debuggerKind),
                                                                         ZigBundle.message("dialog.title.download.debugger"),
                                                                         true,
                                                                         project);

        if (result instanceof DownloadResult.Ok) {
            Notifications.Bus.notify(new Notification(
                    "ZigBrains.Debugger.Warn",
                    ZigBundle.message("notification.title.debugger"),
                    ZigBundle.message("notification.content.debugger.successfully.downloaded"),
                    NotificationType.INFORMATION
            ));
        } else if (result instanceof DownloadResult.Failed) {
            Notifications.Bus.notify(new Notification(
                    "ZigBrains.Project",
                    ZigBundle.message("notification.title.debugger"),
                    ZigBundle.message("notification.content.debugger.downloading.failed"),
                    NotificationType.ERROR
            ));
        }

        return result;
    }

    private DownloadResult downloadDebuggerSynchronously(DebuggerKind kind) {
        val baseDir = basePath(kind);

        val downloadableBinaries = switch (kind) {
            case LLDB -> {
                val urls = lldbUrls();
                if (urls == null)
                    yield null;

                val fwUrl = urls.framework.toString();
                val feUrl = urls.frontend.toString();
                yield List.of(
                        new DownloadableDebuggerBinary(fwUrl, LLDB_FRAMEWORK_PROPERTY_NAME, fileNameWithoutExtension(fwUrl)),
                        new DownloadableDebuggerBinary(feUrl, LLDB_FRONTEND_PROPERTY_NAME, fileNameWithoutExtension(feUrl))
                             );
            }
            case GDB -> {
                val gdbUrl = gdbUrl();
                if (gdbUrl == null)
                    yield null;
                val url = gdbUrl.toString();
                yield List.of(new DownloadableDebuggerBinary(url, GDB_PROPERTY_NAME, fileNameWithoutExtension(url)));
            }
            case MSVC -> {
                val msvcUrl = msvcUrl();
                if (msvcUrl == null)
                    yield null;

                AtomicReference<Boolean> accepted = new AtomicReference<>(false);

                ApplicationManager.getApplication().invokeAndWait(() -> {
                    val dialog = new DialogBuilder();
                    dialog.setTitle(msvcUrl.dialogTitle);
                    dialog.addCancelAction().setText("Reject");
                    dialog.addOkAction().setText("Accept");
                    val centerPanel = new JBPanel<>();
                    val hyperlink = new HyperlinkLabel();
                    hyperlink.setTextWithHyperlink(msvcUrl.dialogBody);
                    hyperlink.setHyperlinkTarget(msvcUrl.dialogLink);
                    hyperlink.addHyperlinkListener(new BrowserHyperlinkListener());
                    centerPanel.add(hyperlink);
                    dialog.centerPanel(centerPanel);
                    accepted.set(dialog.showAndGet());
                });

                if (!accepted.get())
                    yield null;

                yield List.of(new DownloadableDebuggerBinary(msvcUrl.url, MSVC_PROPERTY_NAME, msvcUrl.version, "extension/debugAdapters/vsdbg/bin"));
            }
        };

        if (downloadableBinaries == null)
            return DownloadResult.NoUrls;

        try {
            downloadAndUnarchive(baseDir, downloadableBinaries);
            return new DownloadResult.Ok(baseDir);
        } catch (IOException e) {
            //TODO logging
            e.printStackTrace();
            return new DownloadResult.Failed(e.getMessage());
        }
    }

    private void downloadAndUnarchive(Path baseDir, List<DownloadableDebuggerBinary> binariesToDownload)
            throws IOException {
        val service = DownloadableFileService.getInstance();

        val downloadDir = baseDir.toFile();
        FileUtil.deleteRecursively(baseDir);

        val descriptions = binariesToDownload.stream().map(it -> service.createFileDescription(it.url, fileName(it.url))).toList();

        val downloader = service.createDownloader(descriptions, "Debugger downloading");
        val downloadDirectory = downloadPath().toFile();
        val downloadResults = downloader.download(downloadDirectory);

        val versions = new Properties();
        for (val result: downloadResults) {
            val downloadUrl = result.getSecond().getDownloadUrl();
            val binaryToDownload = binariesToDownload.stream()
                                                     .filter(it -> Objects.equals(it.url, downloadUrl))
                                                     .findFirst()
                                                     .orElseThrow(() -> new IOException("Failed to find matching download URL!"));
            val propertyName = binaryToDownload.propertyName;
            val archiveFile = result.getFirst();
            Unarchiver.unarchive(archiveFile, downloadDir, binaryToDownload.prefix);
            archiveFile.delete();
            versions.put(propertyName, binaryToDownload.version);
        }

        saveVersionsFile(baseDir, versions);
    }

    public Properties loadDebuggerVersions(DebuggerKind kind) {
        return loadVersions(basePath(kind));
    }

    public void saveDebuggerVersions(DebuggerKind kind, Properties versions) {
        saveVersionsFile(basePath(kind), versions);
    }

    private void saveVersionsFile(Path basePath, Properties versions) {
        val file = basePath.resolve(DEBUGGER_VERSIONS);
        try (val writer = Files.newBufferedWriter(file)){
            versions.store(writer, "");
        } catch (IOException e) {
            //TODO logging
            e.printStackTrace();
        }
    }

    private Properties loadVersions(Path basePath) {
        val versions = new Properties();
        val versionsFile = basePath.resolve(DEBUGGER_VERSIONS);

        if (Files.exists(versionsFile)) {
            try(val reader = Files.newBufferedReader(versionsFile)) {
                versions.load(reader);
            } catch (IOException e) {
                //TODO logging
                e.printStackTrace();
            }
        }
        return versions;
    }

    private Path basePath(DebuggerKind kind) {
        return switch (kind) {
            case LLDB -> lldbPath();
            case GDB -> gdbPath();
            case MSVC -> msvcPath();
        };
    }

    static Path downloadPath() {
        return Paths.get(PathManager.getTempPath());
    }

    private static Path lldbPath() {
        return ZigPathManager.pluginDirInSystem().resolve("lldb");
    }

    private static Path gdbPath() {
        return ZigPathManager.pluginDirInSystem().resolve("gdb");
    }

    private static Path msvcPath() {
        return ZigPathManager.pluginDirInSystem().resolve("msvc");
    }

    private LLDBUrls lldbUrls() {
        val lldb = UrlProvider.lldb(OS.CURRENT, CpuArch.CURRENT);
        if (lldb == null)
            return null;

        val lldbFrontend = UrlProvider.lldbFrontend(OS.CURRENT, CpuArch.CURRENT);
        if (lldbFrontend == null)
            return null;

        return new LLDBUrls(lldb, lldbFrontend);
    }

    private record LLDBUrls(URL framework, URL frontend) {}

    private URL gdbUrl() {
        return UrlProvider.gdb(OS.CURRENT, CpuArch.CURRENT);
    }

    private MSVCUrl msvcUrl() {
        String dlKey = switch (CpuArch.CURRENT) {
            case X86 -> "downloadX86";
            case X86_64 -> "downloadX86_64";
            case ARM64 -> "downloadARM64";
            default -> null;
        };
        if (dlKey == null)
            return null;

        val props = msvcProvider.msvcProperties();
        val version = props.getProperty("version");
        val url = props.getProperty(dlKey);
        if (url == null || version == null)
            return null;

        return new MSVCUrl(url, version, props.getProperty("dialogTitle"), props.getProperty("dialogBody"), props.getProperty("dialogLink"));
    }

    private record MSVCUrl(String url, String version, String dialogTitle, String dialogBody, String dialogLink) {}

    private static String fileName(String url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }

    private static String fileNameWithoutExtension(String url) {
        url = fileName(url);
        url = removeSuffix(url, ".zip");
        url = removeSuffix(url, ".tar.gz");
        return url;
    }

    private static String removeSuffix(String str, String suffix) {
        if (str.endsWith(suffix))
            str = str.substring(0, str.length() - suffix.length());
        return str;
    }
    private static final String DEBUGGER_VERSIONS = "versions.properties";
    private static final String LLDB_FRONTEND_PROPERTY_NAME = "lldbFrontend";
    private static final String LLDB_FRAMEWORK_PROPERTY_NAME = "lldbFramework";
    private static final String GDB_PROPERTY_NAME = "gdb";
    private static final String MSVC_PROPERTY_NAME = "msvc";

    public sealed interface DownloadResult {
        record Ok(Path baseDir) implements DownloadResult {}
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        final class NoUrls implements DownloadResult {}
        NoUrls NoUrls = new NoUrls();
        record Failed(String message) implements DownloadResult {}
    }

    private enum Unarchiver {
        ZIP {
            @Override
            protected String extension() {
                return "zip";
            }

            @Override
            protected Decompressor createDecompressor(File file) {
                return new Decompressor.Zip(file);
            }
        },
        TAR {
            @Override
            protected String extension() {
                return "tar.gz";
            }

            @Override
            protected Decompressor createDecompressor(File file) {
                return new Decompressor.Tar(file);
            }
        },
        VSIX {
            @Override
            protected String extension() {
                return "vsix";
            }

            @Override
            protected Decompressor createDecompressor(File file) {
                return new Decompressor.Zip(file);
            }
        };

        protected abstract String extension();
        protected abstract Decompressor createDecompressor(File file);

        static void unarchive(File archivePath, File dst, String prefix) throws IOException {
            val unarchiver = Arrays.stream(values())
                                   .filter(it -> archivePath.getName().endsWith(it.extension()))
                                   .findFirst()
                                   .orElseThrow(() -> new IOException("Failed to find decompressor for file " + archivePath.getName()));
            val dec = unarchiver.createDecompressor(archivePath);
            if (prefix != null) {
                dec.removePrefixPath(prefix);
            }
            dec.extract(dst);
        }
    }

    private record DownloadableDebuggerBinary(String url, String propertyName, String version, String prefix) {
        public DownloadableDebuggerBinary(String url, String propertyName, String version) {
            this(url, propertyName, version, null);
        }
    }
}
