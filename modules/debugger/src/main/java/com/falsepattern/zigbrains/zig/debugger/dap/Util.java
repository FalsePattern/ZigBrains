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

package com.falsepattern.zigbrains.zig.debugger.dap;

import com.falsepattern.zigbrains.common.util.ApplicationUtil;
import com.falsepattern.zigbrains.zig.debugger.ZigDebuggerLanguage;
import com.intellij.execution.ExecutionException;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFileUtil;
import com.jetbrains.cidr.execution.debugger.backend.FileLocation;
import com.jetbrains.cidr.execution.debugger.backend.LLBreakpoint;
import com.jetbrains.cidr.execution.debugger.backend.LLBreakpointLocation;
import com.jetbrains.cidr.execution.debugger.backend.LLFrame;
import com.jetbrains.cidr.execution.debugger.backend.LLInstruction;
import com.jetbrains.cidr.execution.debugger.backend.LLMemoryHunk;
import com.jetbrains.cidr.execution.debugger.backend.LLModule;
import com.jetbrains.cidr.execution.debugger.backend.LLSymbolOffset;
import com.jetbrains.cidr.execution.debugger.backend.LLThread;
import com.jetbrains.cidr.execution.debugger.memory.Address;
import com.jetbrains.cidr.execution.debugger.memory.AddressRange;
import lombok.val;
import org.eclipse.lsp4j.debug.Breakpoint;
import org.eclipse.lsp4j.debug.DisassembledInstruction;
import org.eclipse.lsp4j.debug.Module;
import org.eclipse.lsp4j.debug.ReadMemoryResponse;
import org.eclipse.lsp4j.debug.Source;
import org.eclipse.lsp4j.debug.StackFrame;
import org.eclipse.lsp4j.debug.Thread;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

public class Util {
    public static LLThread threadJBFromDAP(Thread DAPThread) {
        return new LLThread(DAPThread.getId(), null, null, DAPThread.getName(), null);
    }

    public static Thread threadDAPFromJB(LLThread JBThread) {
        val DAPThread = new Thread();
        DAPThread.setId((int) JBThread.getId());
        return DAPThread;
    }

    public static LLBreakpoint breakpointJBFromDAP(Breakpoint DAPBreakpoint) {
        val source = DAPBreakpoint.getSource();
        var sourcePath = source == null ? "": Objects.requireNonNullElseGet(source.getPath(), () -> Objects.requireNonNullElse(source.getOrigin(), "unknown"));
        sourcePath = toJBPath(sourcePath);
        return new LLBreakpoint(DAPBreakpoint.getId(), sourcePath, Objects.requireNonNullElse(DAPBreakpoint.getLine(), 0) - 1, null);
    }

    public static @Nullable LLBreakpointLocation getLocation(Breakpoint DAPBreakpoint) {
        val ref = DAPBreakpoint.getInstructionReference();
        if (ref == null)
            return null;
        val addr = Long.parseLong(ref.substring(2), 16);
        FileLocation fl = null;
        val src = DAPBreakpoint.getSource();
        if (src != null) {
            fl = new FileLocation(src.getPath(), DAPBreakpoint.getLine());
        }
        return new LLBreakpointLocation(DAPBreakpoint.getId() + "", Address.fromUnsignedLong(addr), fl);
    }

    public static Breakpoint breakpointDAPFromJB(LLBreakpoint JBBreakpoint) {
        val DAPBreakpoint = new Breakpoint();
        DAPBreakpoint.setId(JBBreakpoint.getId());
        DAPBreakpoint.setLine(JBBreakpoint.getOrigLine() + 1);
        val source = new Source();
        source.setPath(JBBreakpoint.getOrigFile());
        DAPBreakpoint.setSource(source);
        DAPBreakpoint.setMessage(JBBreakpoint.getCondition());
        return DAPBreakpoint;
    }

    public static LLModule moduleJBFromDAP(Module DAPModule) {
        return new LLModule(toJBPath(DAPModule.getPath()));
    }

    public static Module moduleDAPFromJB(LLModule JBModule) {
        val DAPModule = new Module();
        DAPModule.setPath(toJBPath(JBModule.getPath()));
        DAPModule.setName(JBModule.getName());
        return DAPModule;
    }

    public static LLFrame frameJBFromDAP(StackFrame DAPFrame, @Nullable DAPDriver.MappedBreakpoint helperBreakpoint, Map<Integer, DAPDriver.MappedModule> modules) {
        val ptr = parseAddress(DAPFrame.getInstructionPointerReference());
        val name = DAPFrame.getName();
        boolean inline = name.startsWith("[Inline Frame] ");
        val function = name.substring(name.indexOf('!') + 1, name.indexOf('('));
        val moduleID = DAPFrame.getModuleId();
        String moduleName = null;
        if (moduleID != null) {
            if (moduleID.isRight()) {
                moduleName = moduleID.getRight();
            } else {
                val module = modules.get(moduleID.getLeft());
                moduleName = module.java().getName();
            }
        }
        var line = DAPFrame.getLine();
        String sourcePath;
        {
            val src = DAPFrame.getSource();
            sourcePath = src == null ? null : toJBPath(src.getPath());
        }
        if (helperBreakpoint != null) {
            if (line == 0) {
                line = helperBreakpoint.dap().getLine();
            }
            if (sourcePath == null) {
                val src = helperBreakpoint.dap().getSource();
                if (src != null) {
                    sourcePath = toJBPath(src.getPath());
                }
            }
        }
        return new LLFrame(DAPFrame.getId(),
                           function,
                           sourcePath,
                           null,
                           line - 1,
                           ptr,
                           ZigDebuggerLanguage.INSTANCE,
                           false,
                           inline,
                           moduleName);
    }

    public static Source toSource(String path) {
        val src = new Source();
        val absolute = Path.of(path).toAbsolutePath();
        src.setName(absolute.getFileName().toString());
        src.setPath(toWinPath(absolute.toString()));
        return src;
    }

    public static String toWinPath(String path) {
        if (path == null)
            return null;
        return path.replace('/', '\\');
    }

    public static String toJBPath(String path) {
        if (path == null)
            return null;
        return path.replace('\\', '/');
    }

    public static Long parseAddressNullable(String address) {
        if (address == null)
            return null;
        return parseAddress(address);
    }

    public static long parseAddress(String address) {
        if (address == null)
            return 0L;
        if (!address.startsWith("0x"))
            return Long.parseUnsignedLong(address);
        return Long.parseUnsignedLong(address.substring(2), 16);
    }

    public static String stringifyAddress(long address) {
        return "0x" + Long.toHexString(address);
    }

    private static final Pattern HEX_FIX_REGEX = Pattern.compile("([0-9A-F]+)(?<!\\W)h");
    public static LLInstruction instructionJBFromDAP(DisassembledInstruction DAPInstruction, Source loc, Integer startLine, Integer endLine, boolean uniq, LLSymbolOffset symbol) {
        val address = Address.parseHexString(DAPInstruction.getAddress());
        val byteStrings = DAPInstruction.getInstructionBytes().split(" ");
        val bytes = new ArrayList<Byte>(byteStrings.length);
        for (val byteString: byteStrings) {
            bytes.add((byte) Integer.parseInt(byteString, 16));
        }
        val result = new ArrayList<LLInstruction>();
        String comment = null;
        blk:
        if (loc != null && startLine != null && endLine != null && uniq) {
            val pathStr = Util.toJBPath(loc.getPath());
            Path path;
            try {
                path = Path.of(pathStr);
            } catch (InvalidPathException ignored) {
                break blk;
            }
            val text = ApplicationUtil.computableReadAction(() -> {
                val file = VfsUtil.findFile(path, true);
                if (file == null)
                    return null;
                val doc = VirtualFileUtil.findDocument(file);
                if (doc == null)
                    return null;
                return doc.getImmutableCharSequence().toString().split("(\r\n|\r|\n)");
            });
            if (text == null)
                break blk;
            startLine -= 1;
            endLine -= 1;
            if (text.length <= endLine)
                break blk;
            comment = text[endLine];
        }
        var nicerDisassembly = new StringBuilder();
        var disassembly = DAPInstruction.getInstruction();
        val matcher = HEX_FIX_REGEX.matcher(disassembly);
        int prevEnd = 0;
        while (matcher.find()) {
            nicerDisassembly.append(disassembly, prevEnd, matcher.start());
            val hex = matcher.group(1).toLowerCase();
            nicerDisassembly.append("0x").append(hex);
            prevEnd = matcher.end();
        }
        if (prevEnd < disassembly.length())
            nicerDisassembly.append(disassembly, prevEnd, disassembly.length());
        return LLInstruction.create(address,
                                    bytes,
                                    nicerDisassembly.toString(),
                                    comment,
                                    symbol);
    }

    public static LLMemoryHunk memoryJBFromDAP(ReadMemoryResponse DAPMemory) {
        val address = Util.parseAddress(DAPMemory.getAddress());
        val bytes = Base64.getDecoder().decode(DAPMemory.getData());
        val range = new AddressRange(Address.fromUnsignedLong(address), Address.fromUnsignedLong(address + bytes.length - 1));
        return new LLMemoryHunk(range, bytes);
    }

    public static <T> T get(CompletableFuture<T> future) throws ExecutionException {
        try {
            return future.get(4, TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException e) {
            throw new ExecutionException(e);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new ExecutionException(e.getCause());
        }
    }

    public static @NotNull String emptyIfNull(@Nullable String str) {
        return str == null ? "" : str;
    }
}
