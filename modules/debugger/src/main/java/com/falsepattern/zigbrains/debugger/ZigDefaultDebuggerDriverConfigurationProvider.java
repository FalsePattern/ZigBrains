package com.falsepattern.zigbrains.debugger;

import com.falsepattern.zigbrains.ZigBundle;
import com.falsepattern.zigbrains.debugbridge.ZigDebuggerDriverConfigurationProvider;
import com.falsepattern.zigbrains.debugger.settings.ZigDebuggerSettings;
import com.falsepattern.zigbrains.debugger.toolchain.DebuggerAvailability;
import com.falsepattern.zigbrains.debugger.toolchain.DebuggerAvailability.GDBBinaries;
import com.falsepattern.zigbrains.debugger.toolchain.DebuggerAvailability.LLDBBinaries;
import com.falsepattern.zigbrains.debugger.toolchain.DebuggerAvailability.MSVCBinaries;
import com.falsepattern.zigbrains.debugger.toolchain.DebuggerKind;
import com.falsepattern.zigbrains.debugger.toolchain.ZigDebuggerToolchainService;
import com.falsepattern.zigbrains.debugger.win.MSVCDriverConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DoNotAskOption;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.jetbrains.cidr.ArchitectureType;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration;
import com.jetbrains.cidr.execution.debugger.backend.gdb.GDBDriverConfiguration;
import com.jetbrains.cidr.execution.debugger.backend.lldb.LLDBDriverConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Supplier;

public class ZigDefaultDebuggerDriverConfigurationProvider implements ZigDebuggerDriverConfigurationProvider {
    private static boolean availabilityCheck(Project project, DebuggerKind kind) {
        val service = ZigDebuggerToolchainService.getInstance();
        val availability = service.debuggerAvailability(kind);

        final String message, action;
        switch (availability.kind()) {
            case Bundled, Binaries -> {
                return true;
            }
            case Unavailable -> {
                return false;
            }
            case NeedToDownload -> {
                message = "Debugger is not loaded yet";
                action = "Download";
            }
            case NeedToUpdate -> {
                message = " Debugger is outdated";
                action = "Update";
            }
            default -> throw new AssertionError("This should never happen");
        }

        val downloadDebugger = ZigDebuggerSettings.getInstance().downloadAutomatically || showDialog(project, message, action);

        if (downloadDebugger) {
            val result = ZigDebuggerToolchainService.getInstance().downloadDebugger(project, kind);
            if (result instanceof ZigDebuggerToolchainService.DownloadResult.Ok)
                return true;
        }
        return false;
    }

    private static boolean showDialog(Project project, String message, String action) {
        val doNotAsk = new DoNotAskOption.Adapter() {
            @Override
            public void rememberChoice(boolean isSelected, int exitCode) {
                if (exitCode == Messages.OK) {
                    ZigDebuggerSettings.getInstance().downloadAutomatically = isSelected;
                }
            }
        };

        return MessageDialogBuilder.okCancel(ZigBundle.message("unable.to.run.debugger"), message)
                                   .yesText(action)
                                   .icon(Messages.getErrorIcon())
                                   .doNotAsk(doNotAsk)
                                   .ask(project);
    }

    @Override
    public @Nullable Supplier<DebuggerDriverConfiguration> getDebuggerConfiguration(Project project, boolean isElevated, boolean emulateTerminal) {
        val settings = ZigDebuggerSettings.getInstance();
        val service = ZigDebuggerToolchainService.getInstance();
        val kind = settings.debuggerKind;
        if (!availabilityCheck(project, kind)) {
            return null;
        }
        val availability = service.debuggerAvailability(kind);
        return switch (availability.kind()) {
            case Bundled -> () -> (switch (kind) {
                case LLDB -> new ZigLLDBDriverConfiguration(isElevated, emulateTerminal);
                case GDB -> new ZigGDBDriverConfiguration(isElevated, emulateTerminal);
                case MSVC -> throw new AssertionError("MSVC is never bundled");
            });
            case Binaries -> {
                val bin = (DebuggerAvailability.Binaries) availability;
                yield () -> (switch (bin.binariesKind()) {
                    case LLDB -> new ZigCustomBinariesLLDBDriverConfiguration((LLDBBinaries) bin, isElevated, emulateTerminal);
                    case GDB -> new ZigCustomBinariesGDBDriverConfiguration((GDBBinaries) bin, isElevated, emulateTerminal);
                    case MSVC -> new ZigMSVCDriverConfiguration((MSVCBinaries) bin, isElevated, emulateTerminal);
                });
            }
            case Unavailable, NeedToDownload, NeedToUpdate -> throw new AssertionError("This should never happen");
        };
    }

    //region GDB
    @RequiredArgsConstructor
    public static class ZigGDBDriverConfiguration extends GDBDriverConfiguration {
        private final boolean isElevated;
        private final boolean emulateTerminal;

        @Override
        public @NotNull String getDriverName() {
            return "Zig GDB";
        }

        // TODO: investigate attach to process feature separately
        @Override
        public boolean isAttachSupported() {
            return false;
        }

        @Override
        public boolean isElevated() {
            return isElevated;
        }

        @Override
        public boolean emulateTerminal() {
            return emulateTerminal;
        }
    }

    private static class ZigCustomBinariesGDBDriverConfiguration extends ZigGDBDriverConfiguration {
        private final GDBBinaries binaries;
        public ZigCustomBinariesGDBDriverConfiguration(GDBBinaries binaries, boolean isElevated, boolean emulateTerminal) {
            super(isElevated, emulateTerminal);
            this.binaries = binaries;
        }

        @Override
        protected @NotNull String getGDBExecutablePath() {
            return binaries.gdbFile().toString();
        }
    }
    //endregion GDB

    //region LLDB
    @RequiredArgsConstructor
    public static class ZigLLDBDriverConfiguration extends LLDBDriverConfiguration {
        private final boolean isElevated;
        private final boolean emulateTerminal;

        @Override
        public @NotNull String getDriverName() {
            return "Zig LLDB";
        }

        @Override
        public boolean isElevated() {
            return isElevated;
        }

        @Override
        public boolean emulateTerminal() {
            return emulateTerminal;
        }
    }

    private static class ZigCustomBinariesLLDBDriverConfiguration extends ZigLLDBDriverConfiguration {
        private final LLDBBinaries binaries;
        public ZigCustomBinariesLLDBDriverConfiguration(LLDBBinaries binaries, boolean isElevated, boolean emulateTerminal) {
            super(isElevated, emulateTerminal);
            this.binaries = binaries;
        }

        @Override
        public boolean useSTLRenderers() {
            return false;
        }

        @Override
        protected @NotNull File getLLDBFrameworkFile(@NotNull ArchitectureType architectureType) {
            return binaries.frameworkFile().toFile();
        }

        @Override
        protected @NotNull File getLLDBFrontendFile(@NotNull ArchitectureType architectureType) {
            return binaries.frontendFile().toFile();
        }
    }
    //endregion LLDB

    //region MSVC
    @RequiredArgsConstructor
    public static class ZigMSVCDriverConfiguration extends MSVCDriverConfiguration {
        private final MSVCBinaries binaries;
        private final boolean isElevated;
        private final boolean emulateTerminal;

        @Override
        public @NotNull String getDriverName() {
            return "Zig MSVC";
        }

        @Override
        protected Path getDebuggerExecutable() {
            return binaries.msvcFile();
        }

        @Override
        public boolean isElevated() {
            return isElevated;
        }

        @Override
        public boolean emulateTerminal() {
            return emulateTerminal;
        }
    }

    //endregion MSVC
}
