/*
 * This file is part of ZigBrains.
 *
 * Copyright (C) 2023-2025 FalsePattern
 * All Rights Reserved
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * ZigBrains is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 *
 * ZigBrains is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ZigBrains. If not, see <https://www.gnu.org/licenses/>.
 */

package com.falsepattern.zigbrains.debugger.dap

import com.falsepattern.zigbrains.project.run.ZigProcessHandler
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.execution.process.BaseProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.KeyWithDefaultValue
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.util.progress.getMaybeCancellable
import com.intellij.util.system.CpuArch
import com.jetbrains.cidr.ArchitectureType
import com.jetbrains.cidr.execution.Installer
import com.jetbrains.cidr.execution.debugger.CidrDebuggerSettings
import com.jetbrains.cidr.execution.debugger.backend.*
import com.jetbrains.cidr.execution.debugger.memory.Address
import com.jetbrains.cidr.execution.debugger.memory.AddressRange
import com.jetbrains.cidr.system.HostMachine
import com.jetbrains.cidr.system.LocalHost
import io.ktor.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.asDeferred
import org.eclipse.lsp4j.debug.*
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer
import org.eclipse.lsp4j.jsonrpc.MessageConsumer
import org.eclipse.lsp4j.jsonrpc.debug.DebugLauncher
import org.eclipse.lsp4j.jsonrpc.messages.Either3
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.io.PipedOutputStream
import java.lang.Exception
import java.util.TreeMap
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import kotlin.math.min

abstract class DAPDriver<Server : IDebugProtocolServer, Client : IDebugProtocolClient>(
    handler: Handler,
) : DebuggerDriver(handler) {
    private lateinit var driverName: String
    private lateinit var processHandler: BaseProcessHandler<*>
    protected lateinit var client: Client
    protected lateinit var server: Server
    @Volatile
    protected lateinit var capabilities: Capabilities
    protected lateinit var initializeFuture: Job

    protected val breakpoints = HashMap<Int, MappedBreakpoint>()
    protected val modules = HashMap<Int, MappedModule>()
    private val registerSets = TreeMap<String, List<LLValue>>()

    @Volatile
    private var childProcess: BaseProcessHandler<*>? = null
    @Volatile
    private var processInput: OutputStream? = null
    @Volatile
    private var dummyOutput: ByteArrayOutputStream? = ByteArrayOutputStream()

    private val multiplexer = object: OutputStream() {
        private val inferior get() = processInput ?: dummyOutput
        override fun close() {
            inferior?.close()
        }

        override fun flush() {
            inferior?.flush()
        }

        override fun write(b: Int) {
            inferior?.write(b)
        }

        override fun write(b: ByteArray) {
            inferior?.write(b)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            inferior?.write(b, off, len)
        }
    }


    protected abstract fun createDebuggerClient(): Client
    protected abstract fun getServerInterface(): Class<Server>
    protected abstract fun wrapMessageConsumer(mc: MessageConsumer): MessageConsumer
    protected abstract suspend fun postInitialize(capabilities: Capabilities)

    fun initialize(config: DAPDebuggerDriverConfiguration) {
        driverName = config.driverName
        processHandler = createDebugProcessHandler(config.createDriverCommandLine(this, ArchitectureType.forVmCpuArch(
            CpuArch.CURRENT
        )), config)

        val pipeOutput = PipedOutputStream()
        val pipeInput = BlockingPipedInputStream(pipeOutput, 1024 * 1024)
        processHandler.addProcessListener(object: ProcessAdapter() {
            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                if (ProcessOutputType.isStdout(outputType)) {
                    val text = event.text ?: return
                    pipeOutput.write(text.toByteArray(Charsets.UTF_8))
                    pipeOutput.flush()
                }
            }
        })
        client = createDebuggerClient()
        val executorServer = Executors.newSingleThreadExecutor()
        val launcher = DebugLauncher.createLauncher(client, getServerInterface(), pipeInput, processHandler.processInput, executorServer, this::wrapMessageConsumer)
        server = launcher.remoteProxy
        launcher.startListening()

        val initArgs = InitializeRequestArguments()

        //Identification
        initArgs.clientID = "zigbrains"
        initArgs.clientName = "ZigBrains"

        //IntelliJ editor thing
        initArgs.linesStartAt1 = true
        initArgs.columnsStartAt1 = true

        initArgs.supportsMemoryReferences = true
        initArgs.supportsVariableType = false


        config.customizeInitializeArguments(initArgs)

        initializeFuture = zigCoroutineScope.launch {
            val caps = server.initialize(initArgs).asDeferred().await()
            capabilities = caps
            postInitialize(caps)
        }
    }

    override fun supportsWatchpointLifetime(): Boolean {
        return false
    }

    override fun supportsMemoryWrite(): Boolean {
        return capabilities.supportsWriteMemoryRequest
    }

    override fun writeMemory(address: Address, bytes: ByteArray) {
        val args = WriteMemoryArguments()
        args.memoryReference = "0x${address.unsignedLongValue.toString(16)}"
        args.data = bytes.encodeBase64()
        server.writeMemory(args).getSafe()
    }

    override fun getProcessHandler(): BaseProcessHandler<*> {
        return processHandler
    }

    override fun isInPromptMode(): Boolean {
        return false
    }

    override fun getHostMachine(): HostMachine {
        return LocalHost.INSTANCE
    }

    override fun setValuesFilteringEnabled(p0: Boolean) {

    }

    protected inner class DAPInferior: Inferior() {
        override fun startImpl(): Long {
            server.configurationDone(ConfigurationDoneArguments()).getSafe()
            return -1
        }

        override fun detachImpl() {
            val args = DisconnectArguments()
            server.disconnect(args).getSafe()
        }

        override fun destroyImpl(): Boolean {
            detachImpl()
            return true
        }
    }

    override fun loadForLaunch(installer: Installer, s: String?): Inferior {
        val cli = installer.install()
        val args = HashMap<String, Any>()
        args["program"] = Util.toWinPath(cli.exePath)
        args["cmd"] = cli.workDirectory.toString()
        args["name"] = "CPP Debug"
        args["type"] = "cppvsdbg"
        args["request"] = "launch"
        args["console"] = "integratedTerminal"
        args["logging"] = mapOf("moduleLoad" to true)
        args["__configurationTarget"] = 2
        val params = cli.parametersList.array
        if (params.isNotEmpty()) {
            args["args"] = params
        }
        server.launch(args).getSafe()
        runBlocking {
            initializeFuture.join()
        }
        return DAPInferior()
    }

    override fun loadCoreDump(p0: File, p1: File?, p2: File?, p3: MutableList<PathMapping>): Inferior {
        notSupported()
    }

    override fun loadCoreDump(
        p0: File,
        p1: File?,
        p2: File?,
        p3: MutableList<PathMapping>,
        p4: MutableList<String>
    ): Inferior {
        notSupported()
    }

    override fun loadForAttach(p0: Int): Inferior {
        notSupported()
    }

    override fun loadForAttach(p0: String, p1: Boolean): Inferior {
        notSupported()
    }

    override fun loadForRemote(p0: String, p1: File?, p2: File?, p3: MutableList<PathMapping>): Inferior {
        notSupported()
    }

    /**
     * User presses "Pause Program" button.
     * {@link #handleInterrupted} supposed to be called asynchronously when actual pause happened
     */
    override fun interrupt(): Boolean {
        val pause = PauseArguments()
        pause.threadId = -1
        server.pause(pause).getSafe()
        return true
    }

    override fun resume(): Boolean {
        val args = ContinueArguments()
        server.continue_(args).getSafe()
        return true
    }

    @Deprecated("Inherited from deprecated")
    override fun stepOver(p0: Boolean) {
        deprecated()
    }

    @Deprecated("Inherited from deprecated")
    override fun stepInto(p0: Boolean, p1: Boolean) {
        deprecated()
    }

    @Deprecated("Inherited from deprecated")
    override fun stepOut(p0: Boolean) {
        deprecated()
    }

    override fun stepOver(thread: LLThread, stepByInstruction: Boolean) {
        val args = NextArguments()
        args.threadId = Math.toIntExact(thread.id)
        args.granularity = if (stepByInstruction)
            SteppingGranularity.INSTRUCTION
        else
            SteppingGranularity.LINE
        server.next(args).getSafe()
    }

    override fun stepInto(thread: LLThread, forceStepIntoFramesWithNoDebugInfo: Boolean, stepByInstruction: Boolean) {
        val args = StepInArguments()
        args.threadId = Math.toIntExact(thread.id)
        args.granularity = if (stepByInstruction)
            SteppingGranularity.INSTRUCTION
        else
            SteppingGranularity.LINE
        server.stepIn(args).getSafe()
    }

    override fun stepOut(thread: LLThread, stopInFramesWithNoDebugInfo: Boolean) {
        val args = StepOutArguments()
        args.threadId = Math.toIntExact(thread.id)
        server.stepOut(args).getSafe()
    }

    /**
     * Run to source file line
     *
     * @see #stepOver
     */
    override fun runTo(path: String, line: Int) {
        val targetArgs = GotoTargetsArguments()
        val src = Util.toSource(path)
        targetArgs.source = src
        targetArgs.line = line
        val locations = server.gotoTargets(targetArgs).getSafe()
        val targets = locations.targets.firstOrNull() ?: throw ExecutionException("Could not find runTo target!")
        val args = GotoArguments()
        args.targetId = targets.id
        args.threadId = -1
        server.goto_(args).getSafe()
    }

    /**
     * Run to PC address
     *
     * @see #stepOver
     */
    override fun runTo(address: Address) {
        throw UnsupportedOperationException("RunTo address not implemented!")
    }

    /**
     * Perform debugger exit
     *
     * @see #stepOver
     */
    override fun doExit(): Boolean {
        val disconnectArgs = DisconnectArguments()
        disconnectArgs.terminateDebuggee = true
        server.disconnect(disconnectArgs)
        return true
    }

    /**
     *  "Jump" to support
     */
    override fun jumpToLine(thread: LLThread, path: String, line: Int, canLeaveFunction: Boolean): StopPlace {
        throw DebuggerCommandException("Can't resolve address for line $path:$line")
    }

    /**
     *  "Jump" to support
     */
    override fun jumpToAddress(thread: LLThread, address: Address, canLeaveFunction: Boolean): StopPlace {
        throw DebuggerCommandException("Can't jump to address $address")
    }

    override fun addPathMapping(index: Int, from: String, to: String) {
        throw ExecutionException("addPathMapping not implemented!")
    }

    override fun addForcedFileMapping(index: Int, from: String, hash: DebuggerSourceFileHash?, to: String) {
        addPathMapping(index, from, to)
    }

    /**
     * Autocomplete support for debugger console
     */
    override fun completeConsoleCommand(p0: String, p1: Int): ResultList<String> {
        throw ExecutionException("completeConsoleCommand")
    }

    /**
     * Watchpoint handling
     */
    override fun addWatchpoint(
        threadId: Long,
        frameIndex: Int,
        value: LLValue,
        expr: String,
        lifetime: LLWatchpoint.Lifetime?,
        accessType: LLWatchpoint.AccessType
    ): LLWatchpoint {
        throw ExecutionException("TODO")
    }

    /**
     * Watchpoint handling
     */
    override fun removeWatchpoint(ids: MutableList<Int>) {
        throw ExecutionException("TODO")
    }

    /** User adds a breakpoint
     * {@link #handleBreakpointAdded} supposed to be called asynchronously when done
     */
    override fun addBreakpoint(path: String, line: Int, condition: String?, ignoreSourceHash: Boolean): AddBreakpointResult {
        val bp = SourceBreakpoint()
        bp.line = line + 1
        bp.condition = condition
        val bps = breakpoints.values.filter { it.ref.isFirst && it.ref.first.path == path }
            .map { it.ref.first.src }
            .toMutableList()

        bps.add(bp)
        val bpsRes = updateSourceBreakpoints(path, bps)

        val dapBP = bpsRes.last()

        val mbp = MappedBreakpoint(dapBP, Either3.forFirst(PathedSourceBreakpoint(path, bp)))

        breakpoints.compute(dapBP.id, { _, _ -> mbp})

        return AddBreakpointResult(mbp.java, listOfNotNull(mbp.loc))
    }

    private fun updateSourceBreakpoints(path: String, bps: List<SourceBreakpoint>): Array<Breakpoint> {
        val args = SetBreakpointsArguments()
        val src = Util.toSource(path)
        args.source = src
        args.breakpoints = bps.toTypedArray()
        args.sourceModified = false
        val res = server.setBreakpoints(args).getSafe()
        return res.breakpoints
    }

    /**
     * User adds a symbolic breakpoint
     */
    override fun addSymbolicBreakpoint(symBreakpoint: SymbolicBreakpoint): LLSymbolicBreakpoint? {
        if (!capabilities.supportsFunctionBreakpoints)
            throw DebuggerCommandException("Server doesn't support function breakpoints!")

        val fbp = FunctionBreakpoint()
        fbp.name = symBreakpoint.pattern
        fbp.condition = symBreakpoint.condition

        val bps = breakpoints.values.filter { it.ref.isSecond }.map { it.ref.second }.toMutableList()

        bps.add(fbp)

        val bpsRes = updateSymbolicBreakpoints(bps)

        val dapBP = bpsRes.last()

        val mbp = MappedBreakpoint(dapBP, Either3.forSecond(fbp))

        breakpoints.compute(dapBP.id, {_, _ -> mbp})

        return LLSymbolicBreakpoint(mbp.id)
    }

    private fun updateSymbolicBreakpoints(bps: List<FunctionBreakpoint>): Array<Breakpoint> {
        val args = SetFunctionBreakpointsArguments()
        args.breakpoints = bps.toTypedArray()
        val res = server.setFunctionBreakpoints(args).getSafe()
        return res.breakpoints
    }

    /**
     * User adds an address breakpoint
     */
    override fun addAddressBreakpoint(address: Address, condition: String?): AddBreakpointResult {
        if (!capabilities.supportsInstructionBreakpoints)
            throw DebuggerCommandException("Server doesn't support instruction breakpoints!")
        val ibp = InstructionBreakpoint()
        ibp.instructionReference = Util.stringifyAddress(address.unsignedLongValue)
        ibp.condition = condition
        val bps = breakpoints.values.filter { it.ref.isThird }.map { it.ref.third }.toMutableList()

        bps.add(ibp)

        val bpsRes = updateAddressBreakpoints(bps)

        val dapBP = bpsRes.last()

        val mbp = MappedBreakpoint(dapBP, Either3.forThird(ibp))

        breakpoints.compute(dapBP.id, {_, _ -> mbp})

        return AddBreakpointResult(mbp.java, listOfNotNull(mbp.loc))
    }

    private fun updateAddressBreakpoints(bps: List<InstructionBreakpoint>): Array<Breakpoint> {
        val args = SetInstructionBreakpointsArguments()
        args.breakpoints = bps.toTypedArray()
        return server.setInstructionBreakpoints(args).getSafe().breakpoints
    }

    /**
     * User removes symbolic or line breakpoint.
     *
     * [handleBreakpointRemoved] supposed to be called asynchronously when done
     */
    override fun removeCodepoints(ids: MutableCollection<Int>) {
        val removed = ArrayList<MappedBreakpoint>()
        for (id in ids) {
            breakpoints.remove(id)?.let { removed.add(it) }
        }
        val anySrc = removed.any { it.ref.isFirst }
        val anyFunc = removed.any { it.ref.isSecond }
        val anyAddr = removed.any { it.ref.isThird }
        if (anySrc) {
            val sources = removed.asSequence().filter { it.ref.isFirst }.map { it.ref.first.path }.distinct().toList()
            val bps = breakpoints.values.asSequence().filter { it.ref.isFirst }.map { it.ref.first }.toList()
            for (source in sources) {
                val sourceBps = bps.asSequence().filter { it.path == source }.map { it.src }.toList()
                updateSourceBreakpoints(source, sourceBps)
            }
        }
        if (anyFunc) {
            updateSymbolicBreakpoints(breakpoints.values.asSequence().filter { it.ref.isSecond }.map { it.ref.second }.toList())
        }
        if (anyAddr) {
            updateAddressBreakpoints(breakpoints.values.asSequence().filter { it.ref.isThird }.map { it.ref.third }.toList())
        }
    }

    /**
     * List of threads. For instance, RTOS tasks
     */
    override fun getThreads(): List<LLThread> {
        return server.threads().getSafe().threads.asSequence().map(Util::threadJBFromDAP).toList()
    }

    override fun cancelSymbolsDownload(details: String) {
        throw DebuggerCommandException("cancelSymbolsDownload not implemented")
    }

    /**
     * Stack trace for a thread
     */
    override fun getFrames(thread: LLThread, from: Int, count: Int, untilFirstLineWithCode: Boolean): ResultList<LLFrame> {
        val args = StackTraceArguments()
        args.threadId = Math.toIntExact(thread.id)
        args.startFrame = from
        args.levels = count
        val stackTrace = server.stackTrace(args).getSafe()
        val stackFrames = stackTrace.stackFrames
        val resultList = stackFrames.map { Util.frameJBFromDAP(it, null, modules) }
        return ResultList.create(resultList, false)
    }

    @Deprecated("Inherited from deprecated")
    override fun getVariables(p0: Long, p1: Int): MutableList<LLValue> {
        deprecated()
    }

    override fun getFrameVariables(thread: LLThread, frame: LLFrame): FrameVariables {
        return FrameVariables(getWrappedScopes(frame), true)
    }

    // TODO registers
    override fun supportsRegisters(): Boolean {
        return true
    }

    override fun getRegisters(thread: LLThread, frame: LLFrame): List<LLValue> {
        return registerSets.values
            .asSequence()
            .flatMap { it.asSequence() }
            .toList()
    }

    override fun getRegisters(thread: LLThread, frame: LLFrame, registerNames: Set<String>): List<LLValue> {
        if (registerNames.isEmpty()) {
            return getRegisters(thread, frame)
        }
        return registerSets.values
            .asSequence()
            .flatMap { it.asSequence() }
            .filter { registerNames.contains(it.name.lowercase()) }
            .toList()
    }

    override fun getRegisterSets(): List<LLRegisterSet> {
        return registerSets.entries
            .asSequence()
            .map { LLRegisterSet(it.key, it.value.map(LLValue::getName)) }
            .toList()
    }

    @Throws(ExecutionException::class)
    protected fun getWrappedScopes(frame: LLFrame): List<LLValue> {
        val scopeArgs = ScopesArguments()
        val frameID = frame.index
        scopeArgs.frameId = frameID
        val scopes = server.scopes(scopeArgs).getSafe()
        val result = ArrayList<LLValue>()
        for (scope in scopes.scopes) {
            val ref = scope.variablesReference
            if ("registers" == scope.name.lowercase()) {
                updateRegisters(frameID, ref)
                continue
            }
            result.addAll(getVariables(frameID, scope.variablesReference, null, null))
        }
        return result
    }

    @Throws(ExecutionException::class)
    private fun updateRegisters(frameID: Int, rootRef: Int) {
        val registerGroups = getVariables(frameID, rootRef, null, null)
        registerSets.clear()
        var c = 0
        for (registerGroup in registerGroups) {
            val name = "${c++} - ${registerGroup.name}"
            val ref = registerGroup.getUserData(LLVALUE_CHILDREN_REF)
            if (ref == null || ref == 0)
                continue

            val registers = getVariables(frameID, ref, null, null)
            val renamedRegisters = ArrayList<LLValue>(registers.size)
            for (register in registers) {
                val renamedRegister = LLValue(register.name.lowercase(), register.type, register.displayType, register.address, register.typeClass, register.referenceExpression)
                register.copyUserDataTo(renamedRegister)
                val oldData = renamedRegister.getUserData(LLVALUE_DATA)
                if (oldData != null && HEX_REGEX.matches(oldData.value)) {
                    val newData = LLValueData("0x${oldData.value.lowercase()}", oldData.description, oldData.hasLongerDescription(), oldData.mayHaveChildren(), oldData.isSynthetic)
                    renamedRegister.putUserData(LLVALUE_DATA, newData)
                }
                renamedRegisters.add(renamedRegister)
            }
            registerSets[name] = renamedRegisters
        }
        val arch = architecture
        if (arch == null)
            return

        val toggles = HashMap<String, Boolean>()
        var first = true
        for (registerSet in registerSets.keys) {
            toggles[registerSet] = first
            first = false
        }
        val cds = CidrDebuggerSettings.getInstance()
        val settings = cds.getRegisterSetSettings(arch, driverName)
        if (settings == null || !settings.keys.containsAll(toggles.keys)) {
            cds.setRegisterSetSettings(arch, driverName, toggles)
        }
    }

    override fun getArchitecture(): String? {
        return null
    }

    @Throws(ExecutionException::class)
    protected fun getVariables(frameID: Int, variablesReference: Int, start: Int?, count: Int?): List<LLValue> {
        val variableArgs = VariablesArguments()
        variableArgs.variablesReference = variablesReference
        variableArgs.start = start
        variableArgs.count = count
        val variables = server.variables(variableArgs).getSafe().variables
        val javaVariables = ArrayList<LLValue>(variables.size)
        for (variable in variables) {
            val address = Util.parseAddressNullable(variable.memoryReference)
            val type = variable.type ?: ""
            val truncated = type.replace(ERROR_REGEX, "error{...}")
            val name = variable.name
            val evalName = variable.evaluateName ?: ""
            val childRef = variable.variablesReference
            val knownValue = variable.value
            val llValue = LLValue(name, type, truncated, address, null, evalName)
            llValue.putUserData(LLVALUE_FRAME, frameID)
            llValue.putUserData(LLVALUE_CHILDREN_REF, childRef)
            if (knownValue != null) {
                llValue.putUserData(LLVALUE_DATA, LLValueData(knownValue, null, false, childRef > 0, false))
            }
            javaVariables.add(llValue)
        }
        return javaVariables
    }

    override fun getVariables(thread: LLThread, frame: LLFrame): MutableList<LLValue> {
        return getFrameVariables(thread, frame).variables
    }

    /**
     * Read value of a variable
     */
    override fun getData(value: LLValue): LLValueData {
        var result = ""
        var childrenRef = 0
        var failed = false
        if (value.referenceExpression.isBlank()) {
            failed = true
        } else {
            val args = EvaluateArguments()
            args.context = EvaluateArgumentsContext.VARIABLES
            args.expression = value.referenceExpression
            args.frameId = value.getUserData(LLVALUE_FRAME)
            val res = server.evaluate(args).getSafe()
            childrenRef = res.variablesReference
            if (childrenRef > 0) {
                value.putUserData(LLVALUE_CHILDREN_REF, childrenRef)
            }
            val attribs = res.presentationHint?.attributes
            if (attribs != null) {
                for (attrib in attribs) {
                    if ("failedEvaluation" == attrib) {
                        failed = true
                    }
                }
            }
            result = res.result
        }
        if (failed) {
            val known = value.getUserData(LLVALUE_DATA)
            if (known != null)
                return known
            val cRef = value.getUserData(LLVALUE_CHILDREN_REF)
            if (cRef != null)
                childrenRef = cRef
        }
        return LLValueData(result, null, false, childrenRef > 0, false)
    }

    /**
     * Read description of a variable
     */
    override fun getDescription(value: LLValue, maxLength: Int): String {
        val type = value.type
        val length = min(type.length, maxLength)
        return type.substring(0, length)
    }

    /**
     * Unions, structures, or classes are hierarchical. This method help to obtain the hierarchy
     */
    override fun getChildrenCount(value: LLValue): Int {
        val frame = value.getUserData(LLVALUE_FRAME)
        val childrenRef = value.getUserData(LLVALUE_CHILDREN_REF)
        val children = if (childrenRef == null || frame == null)
            emptyList()
        else
            getVariables(frame, childrenRef, null, null)
        value.putUserData(LLVALUE_CHILDREN, children)
        return children.size
    }

    /**
     * Unions, structures, or classes are hierarchical. This method help to obtain the hierarchy
     */
    override fun getVariableChildren(value: LLValue, from: Int, count: Int): ResultList<LLValue> {
        val size = getChildrenCount(value)
        val children = value.getUserData(LLVALUE_CHILDREN)
        return if (children == null || from > size) {
            ResultList.empty()
        } else if (from + count >= size) {
            ResultList.create(children.subList(from, size), false)
        } else {
            ResultList.create(children.subList(from, from + count), true)
        }
    }

    @Deprecated("Inherited from deprecated")
    override fun evaluate(threadId: Long, frameIndex: Int, expression: String, language: DebuggerLanguage?): LLValue {
        val evalArgs = EvaluateArguments()
        evalArgs.frameId = frameIndex
        evalArgs.expression = expression
        val res = server.evaluate(evalArgs).getSafe()
        val type = res.type ?: "unknown"
        val addr = res.memoryReference?.let { Util.parseAddress(it) }
        val result = LLValue("result", type, addr, null, "")
        result.putUserData(LLVALUE_DATA, LLValueData(res.result, null, false, false, false))
        result.putUserData(LLVALUE_FRAME, frameIndex)
        return result
    }

    @Suppress("DEPRECATION")
    override fun evaluate(thread: LLThread, frame: LLFrame, expression: String, language: DebuggerLanguage?): LLValue {
        return evaluate(thread.id, frame.index, expression, language)
    }

    override fun disassembleFunction(address: Address, fallbackRange: AddressRange): List<LLInstruction> {
        return disassemble(fallbackRange)
    }

    override fun disassemble(range: AddressRange): List<LLInstruction> {
        return runBlocking {
            disassembleSuspend(range)
        }
    }

    private suspend fun disassembleSuspend(range: AddressRange): List<LLInstruction> {
        if (!capabilities.supportsDisassembleRequest)
            throw DebuggerCommandException("disassemble is not supported by debugger!")
        val args = DisassembleArguments()
        args.memoryReference = java.lang.Long.toHexString(range.start.unsignedLongValue)
        args.instructionCount = Math.toIntExact(range.size)
        args.resolveSymbols = true
        val disassembly = server.disassemble(args).getSuspend()
        val dapInstructions = disassembly.instructions
        val jbInstructions = ArrayList<LLInstruction>(dapInstructions.size)
        var loc: Source? = null
        var startLine: Int? = null
        var endLine: Int? = null
        var symbol: String? = null
        var baseOffset = 0L
        for (dapInstruction in dapInstructions) {
            val dapLoc = dapInstruction.location
            val dapStartLine = dapInstruction.line
            val dapEndLine = dapInstruction.endLine
            val dapSymbol = dapInstruction.symbol
            val dapAddr = Util.parseAddress(dapInstruction.address)
            var uniq = true
            if (dapLoc != null) {
                loc = dapLoc
            } else if (startLine != null && dapStartLine == startLine && endLine != null && dapEndLine == endLine) {
                uniq = false
            } else {
                startLine = dapStartLine
                endLine = dapEndLine
            }

            if (dapSymbol != null && dapSymbol != symbol) {
                symbol = dapSymbol
                baseOffset = dapAddr
            }

            val llSymbol = symbol?.let {LLSymbolOffset(symbol, dapAddr - baseOffset)}

            jbInstructions.add(Util.instructionJBFromDAP(dapInstruction, loc, startLine, endLine, uniq, llSymbol))
        }
        return jbInstructions
    }

    override fun dumpMemory(range: AddressRange): List<LLMemoryHunk> {
        if (!capabilities.supportsReadMemoryRequest)
            throw DebuggerCommandException("dumpMemory is not supported by debugger!")

        val start = range.start.unsignedLongValue
        val length = range.size
        val hunks = ArrayList<LLMemoryHunk>((length / (Integer.MAX_VALUE.toLong() + 1)).toInt())
        var offset = 0L
        while (offset < length) {
            val blockLength = Math.toIntExact(min(Integer.MAX_VALUE.toLong(), length - offset))
            val args = ReadMemoryArguments()
            args.memoryReference = Util.stringifyAddress(start + offset)
            args.count = blockLength
            hunks.add(Util.memoryJBFromDAP(server.readMemory(args).getSafe()))
            offset += Integer.MAX_VALUE
        }
        return hunks
    }

    override fun getLoadedModules(): List<LLModule> {
        if (!capabilities.supportsModulesRequest)
            throw DebuggerCommandException("getLoadedModules is not supported by debugger!")
        val args = ModulesArguments()
        val modulesResponse = server.modules(args).getSafe()
        val modules = modulesResponse.modules
        return modules.map(Util::moduleJBFromDAP)
    }

    override fun getModuleSections(module: LLModule): List<LLSection> {
        throw DebuggerCommandException("GetModuleSections is not implemented")
    }

    override fun executeShellCommand(executable: String, params: List<String>?, workingDir: String?, timeoutSecs: Int): ShellCommandResult {
        throw ExecutionException("ExecuteShellCommand is not implemented")
    }

    override fun executeInterpreterCommand(command: String): String {
        return executeInterpreterCommand(-1, -1, command)
    }

    override fun executeInterpreterCommand(threadId: Long, frameIndex: Int, text: String): String {
        val args = EvaluateArguments()
        args.expression = text
        args.frameId = frameIndex
        return server.evaluate(args).getSafe().result
    }

    override fun handleSignal(signalName: String, stop: Boolean, pass: Boolean, notify: Boolean) {
        throw DebuggerCommandException("handleSignal is not implemented")
    }

    override fun getPromptText(): String {
        return ""
    }

    /**
     * Verify if driver is in OK state
     *
     * @throws ExecutionException if something is wrong
     */
    override fun checkErrors() {
        // TODO
    }

    override fun addSymbolsFile(file: File, module: File?) {
        throw ExecutionException("addSymbolsFile not implemented!")
    }

    override fun getProcessInput(): OutputStream? {
        return multiplexer
    }

    override fun getDisasmFlavor(): DisasmFlavor {
        return DisasmFlavor.INTEL
    }

    companion object {
        val LLVALUE_FRAME = Key.create<Int>("DAPDriver.LLVALUE_FRAME")
        val LLVALUE_CHILDREN_REF = KeyWithDefaultValue.create<Int>("DAPDriver.LLVALUE_CHILDREN_REF", 0)
        val LLVALUE_DATA = Key.create<LLValueData>("DAPDriver.LLVALUE_DATA")
        val LLVALUE_CHILDREN = Key.create<List<LLValue>>("DAPDriver.LLVALUE_CHILDREN")
    }

    data class PathedSourceBreakpoint(
        val path: String,
        val src: SourceBreakpoint
    )

    data class MappedBreakpoint(
        val id: Int,
        val java: LLBreakpoint,
        val loc: LLBreakpointLocation?,
        val dap: Breakpoint,
        val ref: Either3<PathedSourceBreakpoint, FunctionBreakpoint, InstructionBreakpoint>
    ) {
        constructor(dap: Breakpoint, ref: Either3<PathedSourceBreakpoint, FunctionBreakpoint, InstructionBreakpoint>) :
                this(dap.id, Util.breakpointJBFromDAP(dap), Util.getLocation(dap), dap, ref)
    }

    data class MappedModule(
        val java: LLModule,
        val dap: Module
    ) {
        constructor(dap: Module): this(Util.moduleJBFromDAP(dap), dap)
    }

    abstract inner class DAPDebuggerClient: IDebugProtocolClient {
        override fun runInTerminal(args: RunInTerminalRequestArguments): CompletableFuture<RunInTerminalResponse> {
            return zigCoroutineScope.async {
                runInTerminalAsync(args)
            }.asCompletableFuture()
        }

        fun runInTerminalAsync(args: RunInTerminalRequestArguments): RunInTerminalResponse {
            val cli = PtyCommandLine(args.args.toList())
            cli.withCharset(Charsets.UTF_8)
            val cwd = args.cwd?.ifBlank { null }?.toNioPathOrNull()
            if (cwd != null) {
                cli.withWorkDirectory(cwd.toFile())
            }
            val childProcess = ZigProcessHandler(cli)
            this@DAPDriver.childProcess = childProcess
            childProcess.addProcessListener(object: ProcessListener {
                override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                    handleTargetOutput(
                        event.text,
                        if (ProcessOutputType.isStdout(outputType)) {
                            ProcessOutputType.STDOUT
                        } else if (ProcessOutputType.isStderr(outputType)) {
                            ProcessOutputType.STDERR
                        } else {
                            ProcessOutputType.SYSTEM
                        }
                    )
                }
            })
            childProcess.startNotify()
            val processInput = childProcess.processInput
            this@DAPDriver.processInput = processInput
            val resp = RunInTerminalResponse()
            resp.shellProcessId = childProcess.process.pid().toInt()
            zigCoroutineScope.launch {
                dummyOutput?.toByteArray()?.let { processInput.write(it) }
                dummyOutput = null
            }
            return resp
        }

        override fun output(args: OutputEventArguments) {
            when(args.category) {
                "stdout" -> handleTargetOutput(args.output, ProcessOutputType.STDOUT)
                "stderr" -> handleTargetOutput(args.output, ProcessOutputType.STDERR)
                else -> handleDebuggerOutput(args.output, ProcessOutputType.STDOUT)
            }
        }

        override fun breakpoint(args: BreakpointEventArguments) {
            val bp = args.breakpoint
            when(args.reason) {
                BreakpointEventArgumentsReason.CHANGED -> {
                    val mbp = updateBP(bp)
                    handleBreakpointUpdated(mbp.java)
                    handleBreakpointLocationsReplaced(mbp.id, listOfNotNull(mbp.loc))
                }
                BreakpointEventArgumentsReason.NEW -> {
                    val mbp = updateBP(bp)
                    handleBreakpointAdded(mbp.java)
                    handleBreakpointLocationsReplaced(mbp.id, listOfNotNull(mbp.loc))
                }
                BreakpointEventArgumentsReason.REMOVED -> {
                    breakpoints.remove(bp.id)
                    handleBreakpointRemoved(bp.id)
                }
            }
        }

        private fun updateBP(bp: Breakpoint): MappedBreakpoint {
            return breakpoints.compute(bp.id, { _, mbp ->
                if (mbp != null)
                    return@compute MappedBreakpoint(bp, mbp.ref)

                val ins = InstructionBreakpoint()
                ins.instructionReference = bp.instructionReference
                return@compute MappedBreakpoint(bp, Either3.forThird(ins))
            })!!
        }

        override fun exited(args: ExitedEventArguments) {
            handleExited(args.exitCode)
        }

        override fun stopped(args: StoppedEventArguments) {
            server.threads().thenAccept { threadsResponse ->
                val threads = threadsResponse.threads
                val thread = if (args.threadId != null) {
                    val id = args.threadId!!
                    threads.asSequence().filter { it.id == id }.first()
                } else {
                    threads.asSequence().sortedBy { it.id }.first()
                }

                val jbThread = Util.threadJBFromDAP(thread)
                val stArgs = StackTraceArguments()
                stArgs.threadId = thread.id
                stArgs.startFrame = 0
                stArgs.levels = 1
                server.stackTrace(stArgs).thenAccept { st ->
                    var helperBreakpoint: MappedBreakpoint? = null
                    val isBreakpoint = "breakpoint" == args.reason
                    if (isBreakpoint) {
                        helperBreakpoint = breakpoints.get(args.hitBreakpointIds[0])
                    }
                    val frame = Util.frameJBFromDAP(st.stackFrames[0], helperBreakpoint, modules)
                    val stopPlace = StopPlace(jbThread, frame)
                    if (isBreakpoint) {
                        handleBreakpoint(stopPlace, args.hitBreakpointIds[0])
                    } else {
                        handleInterrupted(stopPlace)
                    }
                }
            }
        }

        override fun continued(args: ContinuedEventArguments) {
            handleRunning()
        }

        override fun module(args: ModuleEventArguments) {
            val module = args.module
            when(args.reason) {
                ModuleEventArgumentsReason.NEW -> {
                    val mm = MappedModule(module)
                    modules[module.id.left] = mm
                    handleModulesLoaded(listOf(mm.java))
                }
                ModuleEventArgumentsReason.CHANGED -> {
                    val newModule = MappedModule(module)
                    val oldModule = modules.put(module.id.left, newModule)
                    if (oldModule != null) {
                        handleModulesUnloaded(listOf(oldModule.java))
                    }
                    handleModulesLoaded(listOf(newModule.java))
                }
                ModuleEventArgumentsReason.REMOVED -> {
                    val oldModule = modules.remove(module.id.left)
                    if (oldModule != null) {
                        handleModulesUnloaded(listOf(oldModule.java))
                    }
                }
                null -> {}
            }
        }
    }
}

@Throws(ExecutionException::class)
private fun <T> CompletableFuture<T>.getSafe(): T {
    try {
        return getMaybeCancellable()
    } catch (e: Exception) {
        throw ExecutionException(e)
    }
}

private suspend fun <T> CompletableFuture<T>.getSuspend(): T {
    try {
        return asDeferred().await()
    } catch (e: Exception) {
        throw ExecutionException(e)
    }
}

private fun notSupported(): Nothing {
    throw ExecutionException("Not supported")
}

private fun deprecated(): Nothing {
    throw ExecutionException("Deprecated")
}

private val HEX_REGEX = Regex("[0-9a-fA-F]+")
private val ERROR_REGEX = Regex("error\\{.*?}")