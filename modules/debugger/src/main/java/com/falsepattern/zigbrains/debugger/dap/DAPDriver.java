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

package com.falsepattern.zigbrains.debugger.dap;

import com.falsepattern.zigbrains.project.runconfig.ZigProcessHandler;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.PtyCommandLine;
import com.intellij.execution.process.BaseProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutputType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.KeyWithDefaultValue;
import com.intellij.util.LazyInitializer;
import com.intellij.util.system.CpuArch;
import com.jetbrains.cidr.ArchitectureType;
import com.jetbrains.cidr.execution.Installer;
import com.jetbrains.cidr.execution.debugger.CidrDebuggerSettings;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerCommandException;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerSourceFileHash;
import com.jetbrains.cidr.execution.debugger.backend.LLBreakpoint;
import com.jetbrains.cidr.execution.debugger.backend.LLBreakpointLocation;
import com.jetbrains.cidr.execution.debugger.backend.LLFrame;
import com.jetbrains.cidr.execution.debugger.backend.LLInstruction;
import com.jetbrains.cidr.execution.debugger.backend.LLMemoryHunk;
import com.jetbrains.cidr.execution.debugger.backend.LLModule;
import com.jetbrains.cidr.execution.debugger.backend.LLRegisterSet;
import com.jetbrains.cidr.execution.debugger.backend.LLSection;
import com.jetbrains.cidr.execution.debugger.backend.LLSymbolOffset;
import com.jetbrains.cidr.execution.debugger.backend.LLSymbolicBreakpoint;
import com.jetbrains.cidr.execution.debugger.backend.LLThread;
import com.jetbrains.cidr.execution.debugger.backend.LLValue;
import com.jetbrains.cidr.execution.debugger.backend.LLValueData;
import com.jetbrains.cidr.execution.debugger.backend.LLWatchpoint;
import com.jetbrains.cidr.execution.debugger.memory.Address;
import com.jetbrains.cidr.execution.debugger.memory.AddressRange;
import com.jetbrains.cidr.system.HostMachine;
import com.jetbrains.cidr.system.LocalHost;
import lombok.val;
import org.eclipse.lsp4j.debug.Breakpoint;
import org.eclipse.lsp4j.debug.BreakpointEventArguments;
import org.eclipse.lsp4j.debug.BreakpointEventArgumentsReason;
import org.eclipse.lsp4j.debug.Capabilities;
import org.eclipse.lsp4j.debug.ConfigurationDoneArguments;
import org.eclipse.lsp4j.debug.ContinueArguments;
import org.eclipse.lsp4j.debug.ContinuedEventArguments;
import org.eclipse.lsp4j.debug.DisassembleArguments;
import org.eclipse.lsp4j.debug.DisconnectArguments;
import org.eclipse.lsp4j.debug.EvaluateArguments;
import org.eclipse.lsp4j.debug.EvaluateArgumentsContext;
import org.eclipse.lsp4j.debug.ExitedEventArguments;
import org.eclipse.lsp4j.debug.FunctionBreakpoint;
import org.eclipse.lsp4j.debug.GotoArguments;
import org.eclipse.lsp4j.debug.GotoTargetsArguments;
import org.eclipse.lsp4j.debug.InitializeRequestArguments;
import org.eclipse.lsp4j.debug.InstructionBreakpoint;
import org.eclipse.lsp4j.debug.Module;
import org.eclipse.lsp4j.debug.ModuleEventArguments;
import org.eclipse.lsp4j.debug.ModulesArguments;
import org.eclipse.lsp4j.debug.NextArguments;
import org.eclipse.lsp4j.debug.OutputEventArguments;
import org.eclipse.lsp4j.debug.PauseArguments;
import org.eclipse.lsp4j.debug.ReadMemoryArguments;
import org.eclipse.lsp4j.debug.RunInTerminalRequestArguments;
import org.eclipse.lsp4j.debug.RunInTerminalResponse;
import org.eclipse.lsp4j.debug.ScopesArguments;
import org.eclipse.lsp4j.debug.SetBreakpointsArguments;
import org.eclipse.lsp4j.debug.SetFunctionBreakpointsArguments;
import org.eclipse.lsp4j.debug.SetInstructionBreakpointsArguments;
import org.eclipse.lsp4j.debug.Source;
import org.eclipse.lsp4j.debug.SourceBreakpoint;
import org.eclipse.lsp4j.debug.StackTraceArguments;
import org.eclipse.lsp4j.debug.StepInArguments;
import org.eclipse.lsp4j.debug.StepOutArguments;
import org.eclipse.lsp4j.debug.SteppingGranularity;
import org.eclipse.lsp4j.debug.StoppedEventArguments;
import org.eclipse.lsp4j.debug.Thread;
import org.eclipse.lsp4j.debug.VariablesArguments;
import org.eclipse.lsp4j.debug.WriteMemoryArguments;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.jsonrpc.debug.DebugLauncher;
import org.eclipse.lsp4j.jsonrpc.messages.Either3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class DAPDriver<
        Server extends IDebugProtocolServer,
        ServerWrapper extends WrappedDebugServer<Server>,
        Client extends IDebugProtocolClient
        >
        extends DebuggerDriver {
    public static final Key<Integer> LLVALUE_FRAME = Key.create("DAPDriver.LLVALUE_FRAME");
    public static final KeyWithDefaultValue<Integer> LLVALUE_CHILDREN_REF = KeyWithDefaultValue.create("DAPDriver.LLVALUE_CHILDREN_REF", 0);
    public static final Key<LLValueData> LLVALUE_DATA = Key.create("DAPDriver.LLVALUE_DATA");
    public static final Key<List<LLValue>> LLVALUE_CHILDREN = Key.create("DAPDriver.LLVALUE_CHILDREN");

    public final String driverName;
    private final BaseProcessHandler<?> processHandler;
    protected final Client client;
    protected final ServerWrapper server;
    protected volatile Capabilities capabilities;
    private LazyInitializer.LazyValue<CompletableFuture<?>> lazy$initializeFuture;
    private CompletableFuture<?> initializeFuture;
    public DAPDriver(@NotNull DebuggerDriver.Handler handler, DAPDebuggerDriverConfiguration config)
            throws ExecutionException {
        super(handler);
        driverName = config.getDriverName();
        processHandler = createDebugProcessHandler(config.createDriverCommandLine(this, ArchitectureType.forVmCpuArch(
                CpuArch.CURRENT)), config);
        val pipeOutput = new PipedOutputStream();
        PipedInputStream pipeInput;
        try {
            pipeInput = new BlockingPipedInputStream(pipeOutput, 1024 * 1024);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        processHandler.addProcessListener(new ProcessAdapter() {
            @Override
            public void startNotified(@NotNull ProcessEvent event) {
                super.startNotified(event);
            }

            @Override
            public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                val text = event.getText();
                if (text == null)
                    return;
                if (ProcessOutputType.isStdout(outputType)) {
                    try {
                        pipeOutput.write(text.getBytes(StandardCharsets.UTF_8));
                        pipeOutput.flush();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        client = createDebuggerClient();
        val executorServer = Executors.newSingleThreadExecutor();
        val launcher = DebugLauncher.createLauncher(client, getServerInterface(), pipeInput, processHandler.getProcessInput(), executorServer, this::wrapMessageConsumer);
        server = wrapDebugServer(launcher.getRemoteProxy());
        launcher.startListening();

        val initArgs = new InitializeRequestArguments();

        //Identification
        initArgs.setClientID("zigbrains");
        initArgs.setClientName("ZigBrains");

        //IntelliJ editor thing
        initArgs.setLinesStartAt1(true);
        initArgs.setColumnsStartAt1(true);

        initArgs.setSupportsMemoryReferences(true);
        initArgs.setSupportsVariableType(false);

        config.customizeInitializeArguments(initArgs);
        lazy$initializeFuture = LazyInitializer.create(() -> wrapInitialize(server.initialize(initArgs)
                                                                                  .thenApply(caps -> {
                                                                                      capabilities = caps;
                                                                                      return caps;
                                                                                  })));
        DAPDriver$postConstructor$invoke();
    }

    /**
     * Override this and make it a no-op when extending this class to avoid race conditions in wrapInitialize.
     */
    public void DAPDriver$postConstructor$invoke() {
        DAPDriver$postConstructor();
    }

    /**
     * Call this at the end of your constructor
     */
    public final void DAPDriver$postConstructor() {
        initializeFuture = lazy$initializeFuture.get();
    }

    protected abstract Class<Server> getServerInterface();
    protected abstract ServerWrapper wrapDebugServer(Server remoteProxy);

    @Override
    public boolean supportsWatchpointLifetime() {
        return false;
    }

    @Override
    public boolean supportsMemoryWrite() {
        return true;
    }

    @Override
    public void writeMemory(@NotNull Address address, byte[] bytes)
            throws ExecutionException, DebuggerCommandException {
        if (!capabilities.getSupportsWriteMemoryRequest())
            throw new DebuggerCommandException("Memory write is not supported");

        val args = new WriteMemoryArguments();
        args.setMemoryReference(Util.stringifyAddress(address.getUnsignedLongValue()));
        args.setData(Base64.getEncoder().encodeToString(bytes));
        server.writeMemoryNow(args);
    }



    @Override
    public @NotNull BaseProcessHandler<?> getProcessHandler() {
        return processHandler;
    }

    @Override
    public boolean isInPromptMode() {
        return false;
    }

    @Override
    public @NotNull HostMachine getHostMachine() {
        return LocalHost.INSTANCE;
    }

    @Override
    public void setValuesFilteringEnabled(boolean b) throws ExecutionException {

    }

    protected class DAPInferior extends Inferior {
        @Override
        protected long startImpl() throws ExecutionException {
            try {
                server.configurationDone(new ConfigurationDoneArguments()).get();
            } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
                throw new RuntimeException(e);
            }
            return -1;
        }

        @Override
        protected void detachImpl() throws ExecutionException {
            val args = new DisconnectArguments();
            server.disconnect(args);
        }

        @Override
        protected boolean destroyImpl() throws ExecutionException {
            detachImpl();
            return true;
        }
    }

    @Override
    public @NotNull Inferior loadForLaunch(@NotNull Installer installer, @Nullable String s) throws ExecutionException {
        if (initializeFuture == null) {
            throw new IllegalStateException("DAPDriver$postConstructor wasn't called!");
        }
        val cli = installer.install();
        val args = new HashMap<String, Object>();
        args.put("program", Util.toWinPath(cli.getExePath()));
        args.put("cwd", cli.getWorkDirectory().toString());
        args.put("name", "CPP Debug");
        args.put("type", "cppvsdbg");
        args.put("request", "launch");
        args.put("console", "integratedTerminal");
        args.put("logging", Map.of("moduleLoad", true));
        args.put("__configurationTarget", 2);
        val params = cli.getParametersList().getArray();
        if (params.length > 0) {
            args.put("args", params);
        }
        server.launchNow(args);
        Util.get(initializeFuture);
        return new DAPInferior();
    }

    @Override
    public @NotNull Inferior loadCoreDump(@NotNull File coreFile, @Nullable File symbolFile, @Nullable File sysroot, @NotNull List<PathMapping> sourcePathMappings) throws ExecutionException {
        throw new ExecutionException("Not supported");
    }

    @Override
    public @NotNull Inferior loadCoreDump(@NotNull File coreFile, @Nullable File symbolFile, @Nullable File sysroot, @NotNull List<PathMapping> sourcePathMappings, @NotNull List<String> execSearchPaths) throws ExecutionException {
        throw new ExecutionException("Not supported");
    }

    @Override
    public @NotNull Inferior loadForRemote(@NotNull String connectionString, @Nullable File symbolFile, @Nullable File sysroot, @NotNull List<PathMapping> pathMappings) throws ExecutionException {
        throw new ExecutionException("Not supported");
    }

    @Override
    public @NotNull Inferior loadForAttach(int pid) throws ExecutionException {
        throw new ExecutionException("Not supported");
    }

    @Override
    public @NotNull Inferior loadForAttach(@NotNull String name, boolean wait) throws ExecutionException {
        throw new ExecutionException("Attaching by name is not supported");
    }


    /**
     * User presses "Pause Program" button.
     * {@link #handleInterrupted} supposed to be called asynchronously when actual pause happened
     */
    @Override
    public boolean interrupt() throws ExecutionException {
        val pause = new PauseArguments();
        pause.setThreadId(-1);
        server.pause(pause);
        return true;
    }

    @Override
    public boolean resume() throws ExecutionException {
        val args = new ContinueArguments();
        server.continue_(args);
        return true;
    }

    @Override
    @Deprecated
    public void stepOver(boolean stepByInstruction) throws ExecutionException {
        throw new ExecutionException("StepOver(boolean) is deprecated!");
    }

    @Override
    @Deprecated
    public void stepInto(boolean forceStepIntoFramesWithNoDebugInfo, boolean stepByInstruction) throws ExecutionException {
        throw new ExecutionException("StepInto(boolean, boolean) is deprecated!");
    }

    @Override
    @Deprecated
    public void stepOut(boolean stopInFramesWithNoDebugInfo) throws ExecutionException {
        throw new ExecutionException("StepOut(boolean) is deprecated!");
    }

    @Override
    public void stepOver(@NotNull LLThread thread, boolean stepByInstruction) throws ExecutionException {
        val args = new NextArguments();
        args.setThreadId(Math.toIntExact(thread.getId()));
        if (stepByInstruction) {
            args.setGranularity(SteppingGranularity.INSTRUCTION);
        } else {
            args.setGranularity(SteppingGranularity.LINE);
        }
        server.next(args);
    }

    @Override
    public void stepInto(@NotNull LLThread thread, boolean forceStepIntoFramesWithNoDebugInfo, boolean stepByInstruction)
            throws ExecutionException {
        val args = new StepInArguments();
        args.setThreadId(Math.toIntExact(thread.getId()));
        if (stepByInstruction) {
            args.setGranularity(SteppingGranularity.INSTRUCTION);
        } else {
            args.setGranularity(SteppingGranularity.LINE);
        }
        server.stepIn(args);
    }

    @Override
    public void stepOut(@NotNull LLThread thread, boolean stopInFramesWithNoDebugInfo) throws ExecutionException {
        val args = new StepOutArguments();
        args.setThreadId(Math.toIntExact(thread.getId()));
        server.stepOut(args);
    }

    /**
     * Run to source file line
     *
     * @see #stepOver
     */
    @Override
    public void runTo(@NotNull String path, int line) throws ExecutionException {
        val targetArgs = new GotoTargetsArguments();
        val src = Util.toSource(path);
        targetArgs.setSource(src);
        targetArgs.setLine(line);
        server.gotoTargets(targetArgs).thenAccept(locations -> {
            val args = new GotoArguments();
            val target = Arrays.stream(locations.getTargets()).findFirst().orElse(null);
            if (target == null) {
                throw new RuntimeException("Could not find runTo target!");
            }
            args.setTargetId(target.getId());
            args.setThreadId(-1);
            server.goto_(args);
        });
    }

    /**
     * Run to PC address
     *
     * @see #stepOver
     */
    @Override
    public void runTo(@NotNull Address address) throws ExecutionException {
        throw new UnsupportedOperationException("RunTo address not implemented!");
    }

    /**
     * Perform debugger exit
     *
     * @see #stepOver
     */
    @Override
    protected boolean doExit() throws ExecutionException {
        val disconnectArgs = new DisconnectArguments();
        disconnectArgs.setTerminateDebuggee(true);
        server.disconnect(disconnectArgs);
        return true;
    }

    /**
     *  "Jump" to support
     */
    @NotNull
    @Override
    public StopPlace jumpToLine(@NotNull LLThread thread, @NotNull String path, int line, boolean canLeaveFunction) throws ExecutionException, DebuggerCommandException {
        throw new DebuggerCommandException(String.format("Can't resolve address for line %s:%d", path, line));
    }

    /**
     *  "Jump" to support
     */
    @NotNull
    @Override
    public StopPlace jumpToAddress(@NotNull LLThread thread, @NotNull Address address, boolean canLeaveFunction) throws ExecutionException, DebuggerCommandException {
        throw new DebuggerCommandException(String.format("Can't jump to address %s", address));
    }

    @Override
    public void addPathMapping(int index, @NotNull String from, @NotNull String to) throws ExecutionException {
        throw new ExecutionException("addPathMapping not implemented!");
    }

    @Override
    public void addForcedFileMapping(int index, @NotNull String from, @Nullable DebuggerSourceFileHash hash, @NotNull String to) throws ExecutionException {
        addPathMapping(index,from,to);
    }

    /**
     * Autocomplete support for debugger console
     */
    @Override
    public @NotNull ResultList<String> completeConsoleCommand(@NotNull String command, int pos) throws ExecutionException {
        throw new ExecutionException("completeConsoleCommand");
    }

    /**
     * Watchpoint handling
     */
    @Override
    public @NotNull LLWatchpoint addWatchpoint(long threadId,
                                               int frameIndex,
                                               @NotNull LLValue value,
                                               @NotNull String expr,
                                               LLWatchpoint.Lifetime lifetime,
                                               @NotNull LLWatchpoint.AccessType accessType) throws ExecutionException, DebuggerCommandException {
        throw new ExecutionException("TODO");
    }

    /**
     * Watchpoint handling
     */
    @Override
    public void removeWatchpoint(@NotNull List<Integer> ids) throws ExecutionException, DebuggerCommandException {
        throw new ExecutionException("TODO");
    }

    public record PathedSourceBreakpoint(@NotNull String path, @NotNull SourceBreakpoint src) {}

    public record MappedBreakpoint(int id, LLBreakpoint java, @Nullable LLBreakpointLocation loc, Breakpoint dap, Either3<PathedSourceBreakpoint, FunctionBreakpoint, InstructionBreakpoint> ref) {
        public MappedBreakpoint(Breakpoint dap, Either3<PathedSourceBreakpoint, FunctionBreakpoint, InstructionBreakpoint> ref) {
            this(dap.getId(), Util.breakpointJBFromDAP(dap), Util.getLocation(dap), dap, ref);
        }
    }

    protected final Map<Integer, MappedBreakpoint> breakpoints = new HashMap<>();

    public record MappedModule(LLModule java, Module dap) {
        public static MappedModule of(Module dap) {
            return new MappedModule(Util.moduleJBFromDAP(dap), dap);
        }
    }

    protected final Map<Integer, MappedModule> modules = new HashMap<>();
    /** User adds a breakpoint
     * {@link #handleBreakpointAdded} supposed to be called asynchronously when done
     */
    @NotNull
    @Override
    public AddBreakpointResult addBreakpoint(@NotNull String path, int line, @Nullable String condition, boolean ignoreSourceHash) throws ExecutionException, DebuggerCommandException {
        line = line+1;
        val bp = new SourceBreakpoint();
        bp.setLine(line);
        bp.setCondition(condition);
        val bps = new ArrayList<>(breakpoints.values()
                                             .stream()
                                             .filter(b -> b.ref.isFirst() && b.ref.getFirst().path.equals(path))
                                             .map(b -> b.ref.getFirst().src)
                                             .toList());
        bps.add(bp);
        val bpsRes = updateSourceBreakpoints(path, bps);

        val dapBP = bpsRes[bpsRes.length - 1];

        val mbp = new MappedBreakpoint(dapBP, Either3.forFirst(new PathedSourceBreakpoint(path, bp)));

        breakpoints.compute(dapBP.getId(), (id, old) -> mbp);

        return new AddBreakpointResult(mbp.java, mbp.loc == null ? List.of() : List.of(mbp.loc));
    }

    public Breakpoint[] updateSourceBreakpoints(String path, List<SourceBreakpoint> bps) throws ExecutionException {
        val args = new SetBreakpointsArguments();
        val src = Util.toSource(path);
        args.setSource(src);
        args.setBreakpoints(bps.toArray(SourceBreakpoint[]::new));
        args.setSourceModified(false);
        val res = server.setBreakpointsNow(args);
        return res.getBreakpoints();
    }

    /**
     * User adds a symbolic breakpoint
     */
    @Override
    public @Nullable LLSymbolicBreakpoint addSymbolicBreakpoint(@NotNull SymbolicBreakpoint symBreakpoint)
            throws ExecutionException, DebuggerCommandException {
        if (!capabilities.getSupportsFunctionBreakpoints())
            throw new DebuggerCommandException("Server doesn't support function breakpoints!");
        val fbp = new FunctionBreakpoint();
        fbp.setName(symBreakpoint.getPattern());
        fbp.setCondition(symBreakpoint.getCondition());
        val bps = new ArrayList<>(breakpoints.values()
                                             .stream()
                                             .filter(b -> b.ref.isSecond())
                                             .map(b -> b.ref.getSecond())
                                             .toList());
        bps.add(fbp);

        val bpsRes = updateSymbolicBreakpoints(bps);

        val dapBP = bpsRes[bpsRes.length - 1];

        val mbp = new MappedBreakpoint(dapBP, Either3.forSecond(fbp));

        breakpoints.compute(dapBP.getId(), (id, old) -> mbp);

        return new LLSymbolicBreakpoint(mbp.id);
    }

    private Breakpoint[] updateSymbolicBreakpoints(List<FunctionBreakpoint> bps) throws ExecutionException {
        val args = new SetFunctionBreakpointsArguments();
        args.setBreakpoints(bps.toArray(FunctionBreakpoint[]::new));
        val res = server.setFunctionBreakpointsNow(args);
        return res.getBreakpoints();
    }

    /**
     * User adds an address breakpoint
     */
    @NotNull
    @Override
    public AddBreakpointResult addAddressBreakpoint(@NotNull Address address, @Nullable String condition) throws ExecutionException, DebuggerCommandException {
        if (!capabilities.getSupportsInstructionBreakpoints())
            throw new DebuggerCommandException("Server doesn't support instruction breakpoints!");
        val ibp = new InstructionBreakpoint();
        ibp.setInstructionReference(Util.stringifyAddress(address.getUnsignedLongValue()));
        ibp.setCondition(condition);
        val bps = new ArrayList<>(breakpoints.values()
                                             .stream()
                                             .filter(b -> b.ref.isThird())
                                             .map(b -> b.ref.getThird())
                                             .toList());
        bps.add(ibp);
        val bpsRes = updateAddressBreakpoints(bps);

        val dapBP = bpsRes[bpsRes.length - 1];

        val mbp = new MappedBreakpoint(dapBP, Either3.forThird(ibp));

        breakpoints.compute(dapBP.getId(), (id, old) -> mbp);

        return new AddBreakpointResult(mbp.java, mbp.loc == null ? List.of() : List.of(mbp.loc));
    }

    private Breakpoint[] updateAddressBreakpoints(List<InstructionBreakpoint> bps) throws ExecutionException {
        val args = new SetInstructionBreakpointsArguments();
        args.setBreakpoints(bps.toArray(InstructionBreakpoint[]::new));
        val res = server.setInstructionBreakpointsNow(args);
        return res.getBreakpoints();
    }

    /**
     * User removes symbolic or line breakpoint
     * {@link #handleBreakpointRemoved(int)} supposed to be called asynchronously when done
     */
    @Override
    public void removeCodepoints(@NotNull Collection<Integer> ids) throws ExecutionException, DebuggerCommandException {
        val removed = new ArrayList<MappedBreakpoint>();
        for (val id: ids) {
            removed.add(breakpoints.remove(id));
        }
        val sources = removed.stream()
                             .filter(bp -> bp.ref.isFirst())
                             .map(bp -> bp.ref.getFirst().path)
                             .distinct()
                             .toList();
        val anyFunc = removed.stream().anyMatch(bp -> bp.ref.isSecond());
        val anyAddr = removed.stream().anyMatch(bp -> bp.ref.isThird());
        for (val source: sources) {
            val bps = breakpoints.values().stream().filter(bp -> bp.ref.isFirst()).map(bp -> bp.ref.getFirst().src).toList();
            updateSourceBreakpoints(source, bps);
        }
        if (anyFunc) {
            updateSymbolicBreakpoints(breakpoints.values().stream().filter(bp -> bp.ref.isSecond()).map(bp -> bp.ref.getSecond()).toList());
        }
        if (anyAddr) {
            updateAddressBreakpoints(breakpoints.values().stream().filter(bp -> bp.ref.isThird()).map(bp -> bp.ref.getThird()).toList());
        }
    }

    /**
     * List of threads. For instance, RTOS tasks
     */
    @Override
    public @NotNull List<LLThread> getThreads() throws ExecutionException, DebuggerCommandException {
        Thread[] threads;
        try {
            threads = server.threads().get().getThreads();
        } catch (InterruptedException e) {
            throw new ExecutionException(e);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new ExecutionException(e.getCause());
        }
        return Arrays.stream(threads).map(Util::threadJBFromDAP).collect(Collectors.toList());
    }

    @Override
    public void cancelSymbolsDownload(@NotNull String details) throws ExecutionException, DebuggerCommandException {
        throw new DebuggerCommandException("cancelSymbolsDownload not implemented");
    }

    /**
     * Stack trace for a thread
     */
    @Override
    public @NotNull ResultList<LLFrame> getFrames(@NotNull LLThread thread, int from, int count, boolean untilFirstLineWithCode)
            throws ExecutionException, DebuggerCommandException {
        val args = new StackTraceArguments();
        args.setThreadId(Math.toIntExact(thread.getId()));
        args.setStartFrame(from);
        args.setLevels(count);
        val stackTrace = server.stackTraceNow(args);
        val stackFrames = stackTrace.getStackFrames();
        val resultList = new ArrayList<LLFrame>(stackFrames.length);
        for (val stackFrame: stackFrames) {
            resultList.add(Util.frameJBFromDAP(stackFrame, null, modules));
        }
        return ResultList.create(resultList, false);
    }

    /**
     * List of available variables
     */
    @Deprecated
    @Override
    public @NotNull List<LLValue> getVariables(long threadId, int frameIndex) throws ExecutionException, DebuggerCommandException {
        throw new ExecutionException("GetVariables(long, int) is deprecated!");
    }

    // TODO registers
    @Override
    public boolean supportsRegisters() {
        return true;
    }

    private final Map<String, List<LLValue>> registerSets = new TreeMap<>();

    @Override
    public @NotNull List<LLValue> getRegisters(@NotNull LLThread thread, @NotNull LLFrame frame)
            throws ExecutionException, DebuggerCommandException {
        return registerSets.values().stream().flatMap(Collection::stream).toList();
    }

    @Override
    public @NotNull List<LLValue> getRegisters(@NotNull LLThread thread, @NotNull LLFrame frame, @NotNull Set<String> registerNames)
            throws ExecutionException, DebuggerCommandException {
        if (registerNames.isEmpty()) {
            return registerSets.values().stream().flatMap(Collection::stream).toList();
        } else {
            return registerSets.values()
                               .stream()
                               .flatMap(Collection::stream)
                               .filter(reg -> registerNames.contains(reg.getName().toLowerCase()))
                               .toList();
        }
    }

    @Override
    public @NotNull List<LLRegisterSet> getRegisterSets() throws ExecutionException, DebuggerCommandException {
        return registerSets.entrySet()
                           .stream()
                           .map(entry -> new LLRegisterSet(entry.getKey(),
                                                           entry.getValue()
                                                                .stream()
                                                                .map(LLValue::getName)
                                                                .toList()))
                           .toList();
    }

    protected List<LLValue> getWrappedScopes(@NotNull LLFrame frame) throws ExecutionException {
        val scopeArgs = new ScopesArguments();
        val frameID = frame.getIndex();
        scopeArgs.setFrameId(frameID);
        val scopes = server.scopesNow(scopeArgs);
        val result = new ArrayList<LLValue>();
        for (val scope: scopes.getScopes()) {
            val ref = scope.getVariablesReference();
            if ("registers".equalsIgnoreCase(scope.getName())) {
                updateRegisters(frameID, ref);
                continue;
            }
            result.addAll(getVariables(frameID, scope.getVariablesReference(), null, null));
        }
        return result;
    }

    private static final Pattern HEX_REGEX = Pattern.compile("[0-9a-fA-F]+");

    private void updateRegisters(int frameID, int rootRef) throws ExecutionException {
        val registerGroups = getVariables(frameID, rootRef, null, null);
        registerSets.clear();
        int c = 0;
        for (val registerGroup: registerGroups) {
            val name = (c++) + " - " + registerGroup.getName();
            val ref = registerGroup.getUserData(LLVALUE_CHILDREN_REF);
            if (ref == null || ref == 0)
                continue;
            val registers = getVariables(frameID, ref, null, null);
            val renamedRegisters = new ArrayList<LLValue>();
            for (val register: registers) {
                val renamedRegister = new LLValue(register.getName().toLowerCase(), register.getType(), register.getDisplayType(), register.getAddress(), register.getTypeClass(), register.getReferenceExpression());
                register.copyUserDataTo(renamedRegister);
                val oldData = renamedRegister.getUserData(LLVALUE_DATA);
                if (oldData != null && HEX_REGEX.matcher(oldData.getValue()).matches()) {
                    val newData = new LLValueData("0x" + oldData.getValue().toLowerCase(), oldData.getDescription(), oldData.hasLongerDescription(), oldData.mayHaveChildren(), oldData.isSynthetic());
                    renamedRegister.putUserData(LLVALUE_DATA, newData);
                }
                renamedRegisters.add(renamedRegister);
            }
            registerSets.put(name, renamedRegisters);
        }
        val arch = getArchitecture();
        if (arch == null) {
            return;
        }
        val toggles = new HashMap<String, Boolean>();
        boolean first = true;
        for (val registerSet: registerSets.keySet()) {
            toggles.put(registerSet, first);
            first = false;
        }
        val settings = CidrDebuggerSettings.getInstance().getRegisterSetSettings(arch, driverName);
        if (settings == null || !settings.keySet().containsAll(toggles.keySet()))
            CidrDebuggerSettings.getInstance().setRegisterSetSettings(getArchitecture(), driverName, toggles);
    }

    @Override
    public @Nullable String getArchitecture() throws ExecutionException {
        return null;
    }

    protected List<LLValue> getVariables(int frameID, int variablesReference, Integer start, Integer count) throws ExecutionException {
        val javaVariables = new ArrayList<LLValue>();
        val variableArgs = new VariablesArguments();
        variableArgs.setVariablesReference(variablesReference);
        variableArgs.setStart(start);
        variableArgs.setCount(count);
        val variables = server.variablesNow(variableArgs);
        for (val variable : variables.getVariables()) {
            val address = Util.parseAddressNullable(variable.getMemoryReference());
            val type = Util.emptyIfNull(variable.getType());
            val truncated = type.replaceAll("error\\{.*?}", "error{}");
            val name = variable.getName();
            val evalName = Util.emptyIfNull(variable.getEvaluateName());
            val childRef = variable.getVariablesReference();
            val knownValue = variable.getValue();

            val llValue = new LLValue(name, type, truncated, address, null, evalName);
            llValue.putUserData(LLVALUE_FRAME, frameID);
            llValue.putUserData(LLVALUE_CHILDREN_REF, childRef);
            if (knownValue != null) {
                llValue.putUserData(LLVALUE_DATA, new LLValueData(knownValue, null, false, childRef > 0, false));
            }
            javaVariables.add(llValue);
        }
        return javaVariables;
    }

    @Override
    public @NotNull List<LLValue> getVariables(@NotNull LLThread thread, @NotNull LLFrame frame)
            throws ExecutionException, DebuggerCommandException {
        return getWrappedScopes(frame);
    }

    /**
     * Read value of a variable
     */
    @Override
    public @NotNull LLValueData getData(@NotNull LLValue value) throws ExecutionException, DebuggerCommandException {
        String result = "";
        int childrenRef = 0;
        boolean failed = false;
        if (value.getReferenceExpression().isBlank()) {
            failed = true;
        } else {
            val args = new EvaluateArguments();
            args.setContext(EvaluateArgumentsContext.VARIABLES);
            args.setExpression(value.getReferenceExpression());
            args.setFrameId(value.getUserData(LLVALUE_FRAME));
            val res = server.evaluateNow(args);
            childrenRef = res.getVariablesReference();
            if (childrenRef > 0)
                value.putUserData(LLVALUE_CHILDREN_REF, childrenRef);
            val hint = res.getPresentationHint();
            if (hint != null) {
                val attribs = hint.getAttributes();
                if (attribs != null) {
                    for (val attrib: attribs) {
                        if ("failedEvaluation".equals(attrib)) {
                            failed = true;
                        }
                    }
                }
            }
            result = res.getResult();
        }
        if (failed) {
            val known = value.getUserData(LLVALUE_DATA);
            if (known != null)
                return known;
            val cRef = value.getUserData(LLVALUE_CHILDREN_REF);
            if (cRef != null)
                childrenRef = cRef;
        }
        return new LLValueData(result, null, false, childrenRef > 0, false);
    }

    /**
     * Read description of a variable
     */
    @Override
    public @Nullable String getDescription(@NotNull LLValue value, int maxLength) throws ExecutionException, DebuggerCommandException {
        val type = value.getType();
        val length = Math.min(type.length(), maxLength);
        return type.substring(0, length);
    }

    /**
     * Unions, structures, or classes are hierarchical. This method help to obtain the hierarchy
     */
    @Override
    public @Nullable Integer getChildrenCount(@NotNull LLValue value) throws ExecutionException, DebuggerCommandException {
        val frame = value.getUserData(LLVALUE_FRAME);
        val childrenRef = value.getUserData(LLVALUE_CHILDREN_REF);
        List<LLValue> children;
        if (childrenRef == null || frame == null) {
            children = List.of();
        } else {
            children = getVariables(frame, childrenRef, null, null);
        }
        value.putUserData(LLVALUE_CHILDREN, children);
        return children.size();
    }

    /**
     * Unions, structures, or classes are hierarchical. This method help to obtain the hierarchy
     */
    @Override
    public @NotNull ResultList<LLValue> getVariableChildren(@NotNull LLValue value, int from, int count)
            throws ExecutionException, DebuggerCommandException {
        val size = getChildrenCount(value);
        val children = value.getUserData(LLVALUE_CHILDREN);
        if (children == null || size == null || from > size) {
            return new ResultList<>(List.of(), false);
        }
        if (from + count >= size) {
            return new ResultList<>(children.subList(from, size), false);
        } else {
            return new ResultList<>(children.subList(from, from + count), true);
        }
    }

    /**
     * Expression evaluation
     */
    @Deprecated
    @Override
    public @NotNull LLValue evaluate(long threadId, int frameIndex, @NotNull String expression, @Nullable DebuggerLanguage language)
            throws ExecutionException, DebuggerCommandException {
        val evalArgs = new EvaluateArguments();
        evalArgs.setFrameId(frameIndex);
        evalArgs.setExpression(expression);
        val res = server.evaluateNow(evalArgs);
        var type = res.getType();
        type = type == null ? "unknown" : type;
        val mRef = res.getMemoryReference();
        Long addr = mRef == null ? null : Util.parseAddress(mRef);
        val result = new LLValue("result", type, addr, null, "");
        result.putUserData(LLVALUE_DATA, new LLValueData(res.getResult(), null, false, false, false));
        result.putUserData(LLVALUE_FRAME, frameIndex);
        return result;
    }

    @Override
    public @NotNull LLValue evaluate(@NotNull LLThread thread, @NotNull LLFrame frame, @NotNull String expression, @Nullable DebuggerDriver.DebuggerLanguage language)
            throws ExecutionException, DebuggerCommandException {
        return super.evaluate(thread, frame, expression, language);
    }

    @Override
    public @NotNull List<LLInstruction> disassembleFunction(@NotNull Address address, @NotNull AddressRange fallbackRange)
            throws ExecutionException, DebuggerCommandException {
        return disassemble(fallbackRange);
    }

    @Override
    public @NotNull List<LLInstruction> disassemble(@NotNull AddressRange range) throws ExecutionException, DebuggerCommandException {
        if (!capabilities.getSupportsDisassembleRequest())
            throw new DebuggerCommandException("disassemble is not supported by debugger!");
        val args = new DisassembleArguments();
        args.setMemoryReference(Long.toHexString(range.getStart().getUnsignedLongValue()));
        args.setInstructionCount(Math.toIntExact(range.getSize()));
        args.setResolveSymbols(true);
        val disassembly = server.disassembleNow(args);
        val dapInstructions = disassembly.getInstructions();
        val jbInstructions = new ArrayList<LLInstruction>(dapInstructions.length);
        Source loc = null;
        Integer startLine = null;
        Integer endLine = null;
        String symbol = null;
        long baseOffset = 0;
        for (val dapInstruction: dapInstructions) {
            val dapLoc = dapInstruction.getLocation();
            val dapStartLine = dapInstruction.getLine();
            val dapEndLine = dapInstruction.getEndLine();
            val dapSymbol = dapInstruction.getSymbol();
            val dapAddr = Util.parseAddress(dapInstruction.getAddress());
            boolean uniq = true;
            if (dapLoc != null) {
                loc = dapLoc;
            } else if (startLine != null && Objects.equals(dapStartLine, startLine) &&
                       endLine != null && Objects.equals(dapEndLine, endLine)) {
                uniq = false;
            } else {
                startLine = dapStartLine;
                endLine = dapEndLine;
            }

            if (dapSymbol != null && !Objects.equals(dapSymbol, symbol)) {
                symbol = dapSymbol;
                baseOffset = dapAddr;
            }

            val llSymbol = symbol == null ? null : new LLSymbolOffset(symbol, dapAddr - baseOffset);

            jbInstructions.add(Util.instructionJBFromDAP(dapInstruction, loc, startLine, endLine, uniq, llSymbol));
        }
        return jbInstructions;
    }

    @Override
    public @NotNull List<LLMemoryHunk> dumpMemory(@NotNull AddressRange range) throws ExecutionException, DebuggerCommandException {
        if (!capabilities.getSupportsReadMemoryRequest())
            throw new DebuggerCommandException("dumpMemory is0 not supported by debugger!");
        val start = range.getStart().getUnsignedLongValue();
        val length = range.getSize();
        val hunks = new ArrayList<LLMemoryHunk>((int) (length / (long)Integer.MAX_VALUE + 1));
        for (long offset = 0; offset < length; offset += Integer.MAX_VALUE) {
            val blockLength = Math.toIntExact(Math.min(Integer.MAX_VALUE, length - offset));
            val args = new ReadMemoryArguments();
            args.setMemoryReference(Util.stringifyAddress(start + offset));
            args.setCount(blockLength);
            hunks.add(Util.memoryJBFromDAP(server.readMemoryNow(args)));
        }
        return hunks;
    }



    @Override
    public @NotNull List<LLModule> getLoadedModules() throws ExecutionException, DebuggerCommandException {
        if (!capabilities.getSupportsModulesRequest())
            throw new DebuggerCommandException("getLoadedModules is not supported by debugger!");
        val args = new ModulesArguments();
        val modulesResponse = server.modulesNow(args);
        val modules = modulesResponse.getModules();
        val javaModules = new ArrayList<LLModule>(modules.length);
        for (val module: modules) {
            javaModules.add(Util.moduleJBFromDAP(module));
        }
        return javaModules;
    }

    @Override
    public @NotNull List<LLSection> getModuleSections(@NotNull LLModule module) throws ExecutionException, DebuggerCommandException {
        throw new DebuggerCommandException("GetModuleSections is not implemented");
    }

    @Override
    public @NotNull ShellCommandResult executeShellCommand(@NotNull String executable,
                                                           @Nullable List<String> params,
                                                           @Nullable String workingDir,
                                                           int timeoutSecs) throws ExecutionException {
        throw new ExecutionException("ExecuteShellCommand is not implemented");
    }

    @Override
    @TestOnly
    public @NotNull String executeInterpreterCommand(@NotNull String command) throws ExecutionException, DebuggerCommandException {
        return executeInterpreterCommand(-1, -1, command);
    }

    @Override
    public @NotNull String executeInterpreterCommand(long threadId, int frameIndex, @NotNull String text) throws ExecutionException, DebuggerCommandException {
        val args = new EvaluateArguments();
        args.setExpression(text);
        args.setFrameId(frameIndex);
        return server.evaluateNow(args).getResult();
    }

    @Override
    public void handleSignal(@NotNull String signalName, boolean stop, boolean pass, boolean notify)
            throws ExecutionException, DebuggerCommandException {
        throw new DebuggerCommandException("handleSignal is not implemented");
    }

    @Override
    protected String getPromptText() {
        return "";
    }

    /**
     * Verify if driver is in OK state
     *
     * @throws ExecutionException if something is wrong
     */
    @Override
    public void checkErrors() throws ExecutionException {
        //todo
    }

    /**
     * Load compiled binary with debug information into debugger engine(but not into target platform)
     */
    @Override
    public void addSymbolsFile(@NotNull File file, File module) throws ExecutionException {
        throw new ExecutionException("addSymbolsFile not implemented!");
    }

    protected abstract Client createDebuggerClient();

    protected abstract CompletableFuture<?> wrapInitialize(CompletableFuture<Capabilities> capabilitiesCompletableFuture);

    protected MessageConsumer wrapMessageConsumer(MessageConsumer messageConsumer) {
        return messageConsumer;
    }

    private volatile BaseProcessHandler<?> childProcess;
    private volatile OutputStream processInput;
    private volatile ByteArrayOutputStream dummyOutput = new ByteArrayOutputStream();

    private final OutputStream multiplexer = new OutputStream() {
        private OutputStream inferior() {
            return processInput != null ? processInput : dummyOutput;
        }
        @Override
        public void write(int b) throws IOException {
            inferior().write(b);
        }

        @Override
        public void write(byte @NotNull [] b) throws IOException {
            inferior().write(b);
        }

        @Override
        public void write(byte @NotNull [] b, int off, int len) throws IOException {
            inferior().write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            inferior().flush();
        }

        @Override
        public void close() throws IOException {
            inferior().close();
        }
    };

    @Override
    public @Nullable OutputStream getProcessInput() {
        return multiplexer;
    }

    protected abstract class DAPDebuggerClient implements IDebugProtocolClient {

        @Override
        public CompletableFuture<RunInTerminalResponse> runInTerminal(RunInTerminalRequestArguments args) {
            val result = new CompletableFuture<RunInTerminalResponse>();
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                val cli = new PtyCommandLine(List.of(args.getArgs()));
                cli.setCharset(StandardCharsets.UTF_8);
                val cwd = args.getCwd();
                if (cwd != null && !cwd.isBlank()) {
                    cli.withWorkDirectory(cwd);
                }
                try {
                    childProcess = new ZigProcessHandler(cli);
                    childProcess.addProcessListener(new ProcessListener() {
                        @Override
                        public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                            if (ProcessOutputType.isStdout(outputType)) {
                                handleTargetOutput(event.getText(), ProcessOutputType.STDOUT);
                            } else if (ProcessOutputType.isStderr(outputType)) {
                                handleTargetOutput(event.getText(), ProcessOutputType.STDERR);
                            } else {
                                handleTargetOutput(event.getText(), ProcessOutputType.SYSTEM);
                            }
                        }

                        @Override
                        public void processTerminated(@NotNull ProcessEvent event) {
                            handleExited(event.getExitCode());
                        }
                    });
                    childProcess.startNotify();
                    processInput = childProcess.getProcessInput();
                    val resp = new RunInTerminalResponse();
                    resp.setShellProcessId((int) childProcess.getProcess().pid());
                    result.complete(resp);
                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        try {
                            processInput.write(dummyOutput.toByteArray());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        dummyOutput = null;
                    });
                } catch (ExecutionException e) {
                    result.completeExceptionally(e);
                    handleDebuggerOutput(e.getMessage(), ProcessOutputType.SYSTEM);
                }
            });
            return result;
        }

        @Override
        public void output(OutputEventArguments args) {
            switch (args.getCategory()) {
                case "stdout" -> handleTargetOutput(args.getOutput(), ProcessOutputType.STDOUT);
                case "stderr" -> handleTargetOutput(args.getOutput(), ProcessOutputType.STDERR);
                default -> handleDebuggerOutput(args.getOutput(), ProcessOutputType.STDOUT);
            }
        }

        @Override
        public void breakpoint(BreakpointEventArguments args) {
            val bp = args.getBreakpoint();
            switch (args.getReason()) {
                case BreakpointEventArgumentsReason.CHANGED -> {
                    val mbp = updateBP(bp);
                    handleBreakpointUpdated(mbp.java);
                    handleBreakpointLocationsReplaced(mbp.id, mbp.loc == null ? List.of() : List.of(mbp.loc));
                }
                case BreakpointEventArgumentsReason.NEW -> {
                    val mbp = updateBP(bp);
                    handleBreakpointAdded(mbp.java);
                    handleBreakpointLocationsReplaced(mbp.id, mbp.loc == null ? List.of() : List.of(mbp.loc));
                }
                case BreakpointEventArgumentsReason.REMOVED -> {
                    breakpoints.remove(bp.getId());
                    handleBreakpointRemoved(bp.getId());
                }
            }
        }

        private MappedBreakpoint updateBP(Breakpoint bp) {
            return breakpoints.compute(bp.getId(), (id, mbp$) -> {
                if (mbp$ == null) {
                    val ins = new InstructionBreakpoint();
                    ins.setInstructionReference(bp.getInstructionReference());
                    return new MappedBreakpoint(bp, Either3.forThird(ins));
                } else {
                    return new MappedBreakpoint(bp, mbp$.ref);
                }
            });
        }

        @Override
        public void exited(ExitedEventArguments args) {
            if (childProcess == null) {
                handleExited(args.getExitCode());
            }
        }

        @Override
        public void stopped(StoppedEventArguments args) {
            server.threads().thenAccept(threadsResponse -> {
                val threads = threadsResponse.getThreads();
                Thread thread;
                if (args.getThreadId() != null) {
                    val id = args.getThreadId();
                    thread = Arrays.stream(threads).filter(t -> t.getId() == id).findFirst().get();
                } else {
                    thread = Arrays.stream(threads).sorted(Comparator.comparingInt(Thread::getId)).findFirst().get();
                }
                val jbThread = Util.threadJBFromDAP(thread);
                val stArgs = new StackTraceArguments();
                stArgs.setThreadId(thread.getId());
                stArgs.setStartFrame(0);
                stArgs.setLevels(1);
                server.stackTrace(stArgs).thenAccept(st -> {
                    MappedBreakpoint helperBreakpoint = null;
                    boolean isBreakpoint = "breakpoint".equals(args.getReason());
                    if (isBreakpoint) {
                        helperBreakpoint = breakpoints.get(args.getHitBreakpointIds()[0]);
                    }
                    val frame = Util.frameJBFromDAP(st.getStackFrames()[0], helperBreakpoint, modules);
                    val stopPlace = new StopPlace(jbThread, frame);
                    if (isBreakpoint) {
                        handleBreakpoint(stopPlace, args.getHitBreakpointIds()[0]);
                    } else {
                        handleInterrupted(stopPlace);
                    }
                });
            });
        }

        @Override
        public void continued(ContinuedEventArguments args) {
            handleRunning();
        }

        @Override
        public void module(ModuleEventArguments args) {
            val module = args.getModule();
            switch (args.getReason()) {
                case NEW -> {
                    val mm = MappedModule.of(module);
                    modules.put(module.getId().getLeft(), mm);
                    handleModulesLoaded(List.of(mm.java));
                }
                case CHANGED -> {
                    val newModule = MappedModule.of(module);
                    val oldModule = modules.put(module.getId().getLeft(), newModule);
                    if (oldModule != null) {
                        handleModulesUnloaded(List.of(oldModule.java));
                    }
                    handleModulesLoaded(List.of(newModule.java));
                }
                case REMOVED -> {
                    val oldModule = modules.remove(module.getId().getLeft());
                    if (oldModule != null) {
                        handleModulesUnloaded(List.of(oldModule.java));
                    }
                }
            }
        }
    }

    @Nullable
    @Override
    public DisasmFlavor getDisasmFlavor() {
        return DisasmFlavor.INTEL;
    }
}
