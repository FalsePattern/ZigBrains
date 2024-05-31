package com.falsepattern.zigbrains.debugger.toolchain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.nio.file.Path;

public sealed interface DebuggerAvailability {
    enum Kind {
        Unavailable,
        NeedToDownload,
        NeedToUpdate,
        Bundled,
        Binaries
    }

    Kind kind();
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    final class Unavailable implements DebuggerAvailability {
        @Override
        public Kind kind() {
            return Kind.Unavailable;
        }
    }
    Unavailable Unavailable = new Unavailable();

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    final class NeedToDownload implements DebuggerAvailability {
        @Override
        public Kind kind() {
            return Kind.NeedToDownload;
        }
    }
    NeedToDownload NeedToDownload = new NeedToDownload();

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    final class NeedToUpdate implements DebuggerAvailability {
        @Override
        public Kind kind() {
            return Kind.NeedToUpdate;
        }
    }
    NeedToUpdate NeedToUpdate = new NeedToUpdate();

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    final class Bundled implements DebuggerAvailability {
        @Override
        public Kind kind() {
            return Kind.Bundled;
        }
    }
    Bundled Bundled = new Bundled();

    sealed interface Binaries extends DebuggerAvailability {
        @Override
        default Kind kind() {
            return Kind.Binaries;
        }

        DebuggerKind binariesKind();
    }

    record LLDBBinaries(Path frameworkFile, Path frontendFile) implements Binaries {
        @Override
        public DebuggerKind binariesKind() {
            return DebuggerKind.LLDB;
        }
    }
    record GDBBinaries(Path gdbFile) implements Binaries {
        @Override
        public DebuggerKind binariesKind() {
            return DebuggerKind.GDB;
        }
    }
    record MSVCBinaries(Path msvcFile) implements Binaries {
        @Override
        public DebuggerKind binariesKind() {
            return DebuggerKind.MSVC;
        }
    }
}
