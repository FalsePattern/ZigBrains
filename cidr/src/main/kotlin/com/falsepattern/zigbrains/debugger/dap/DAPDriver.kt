/*
 * This file is part of ZigBrains.
 *
 * Copyright (C) 2023-2024 FalsePattern
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

import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.execution.ExecutionException
import com.intellij.execution.process.BaseProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.KeyWithDefaultValue
import com.intellij.util.progress.getMaybeCancellable
import com.intellij.util.system.CpuArch
import com.jetbrains.cidr.ArchitectureType
import com.jetbrains.cidr.execution.Installer
import com.jetbrains.cidr.execution.debugger.backend.*
import com.jetbrains.cidr.execution.debugger.memory.Address
import com.jetbrains.cidr.system.HostMachine
import com.jetbrains.cidr.system.LocalHost
import io.ktor.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asDeferred
import org.eclipse.lsp4j.debug.*
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer
import org.eclipse.lsp4j.jsonrpc.MessageConsumer
import org.eclipse.lsp4j.jsonrpc.debug.DebugLauncher
import org.eclipse.lsp4j.jsonrpc.messages.Either3
import java.io.File
import java.io.PipedOutputStream
import java.lang.Exception
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import kotlin.io.path.pathString

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

    override fun interrupt(): Boolean {
        val pause = PauseArguments()
        pause.threadId = -1
        server.pause(pause)
        return true
    }

    override fun resume(): Boolean {
        val args = ContinueArguments()
        server.continue_(args)
        return true
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun stepOver(p0: Boolean) {
        deprecated()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun stepInto(p0: Boolean, p1: Boolean) {
        deprecated()
    }

    @Suppress("OVERRIDE_DEPRECATION")
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
        server.next(args)
    }

    override fun stepInto(thread: LLThread, forceStepIntoFramesWithNoDebugInfo: Boolean, stepByInstruction: Boolean) {
        val args = StepInArguments()
        args.targetId = Math.toIntExact(thread.id)
        args.granularity = if (stepByInstruction)
            SteppingGranularity.INSTRUCTION
        else
            SteppingGranularity.LINE
        server.stepIn(args)
    }

    override fun stepOut(thread: LLThread, stopInFramesWithNoDebugInfo: Boolean) {
        val args = StepOutArguments()
        args.threadId = Math.toIntExact(thread.id)
        server.stepOut(args)
    }

    override fun runTo(path: String, line: Int) {
        val targetArgs = GotoTargetsArguments()
        val src = Util.toSource(path)
        targetArgs.source = src
        targetArgs.line = line
        zigCoroutineScope.launch {
            val locations = server.gotoTargets(targetArgs).asDeferred().await()
            val targets = locations.targets.firstOrNull() ?: throw RuntimeException("Could not find runTo target!")
            val args = GotoArguments()
            args.targetId = targets.id
            args.threadId = -1
            server.goto_(args)
        }
    }

    override fun runTo(p0: Address) {
        throw UnsupportedOperationException("RunTo address not implemented!")
    }

    override fun doExit(): Boolean {
        val disconnectArgs = DisconnectArguments()
        disconnectArgs.terminateDebuggee = true
        server.disconnect(disconnectArgs)
        return true
    }

    // TODO
    override fun jumpToLine(thread: LLThread, path: String, line: Int, canLeaveFunction: Boolean): StopPlace {
        throw DebuggerCommandException("Can't resolve address for line $path:$line")
    }

    // TODO
    override fun jumpToAddress(thread: LLThread, address: Address, canLeaveFunction: Boolean): StopPlace {
        throw DebuggerCommandException("Can't jump to address $address")
    }

    override fun addPathMapping(index: Int, from: String, to: String) {
        throw ExecutionException("addPathMapping not implemented!")
    }

    override fun addForcedFileMapping(index: Int, from: String, hash: DebuggerSourceFileHash?, to: String) {
        addPathMapping(index, from, to)
    }

    override fun completeConsoleCommand(p0: String, p1: Int): ResultList<String> {
        throw ExecutionException("completeConsoleCommand")
    }

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

    override fun removeWatchpoint(ids: MutableList<Int>) {
        throw ExecutionException("TODO")
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

    protected val breakpoints = HashMap<Int, MappedBreakpoint>()

    data class MappedModule(
        val java: LLModule,
        val dap: Module
    ) {
        constructor(dap: Module): this(Util.moduleJBFromDAP(dap), dap)
    }

    protected val modules = HashMap<Int, MappedModule>()

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

    companion object {
        val LLVALUE_FRAME = Key.create<Int>("DAPDriver.LLVALUE_FRAME")
        val LLVALUE_CHILDREN_REF = KeyWithDefaultValue.create<Int>("DAPDriver.LLVALUE_CHILDREN_REF", 0)
        val LLVALUE_DATA = Key.create<LLValueData>("DAPDriver.LLVALUE_DATA")
        val LLVALUE_CHILDREN = Key.create<List<LLValue>>("DAPDriver.LLVALUE_CHILDREN")
    }
}

@Throws(ExecutionException::class)
fun <T> CompletableFuture<T>.getSafe(): T {
    try {
        return getMaybeCancellable()
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