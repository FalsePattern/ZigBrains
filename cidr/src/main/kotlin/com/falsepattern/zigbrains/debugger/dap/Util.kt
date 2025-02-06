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

import com.falsepattern.zigbrains.debugger.ZigDebuggerLanguage
import com.intellij.openapi.application.readAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.findDocument
import com.jetbrains.cidr.execution.debugger.backend.*
import com.jetbrains.cidr.execution.debugger.memory.Address
import com.jetbrains.cidr.execution.debugger.memory.AddressRange
import org.eclipse.lsp4j.debug.*
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.regex.Pattern

object Util {
    fun threadJBFromDAP(DAPThread: Thread): LLThread {
        return LLThread(DAPThread.id.toLong(), null, null, DAPThread.name, null)
    }

    fun threadDAPFromJB(JBThread: LLThread): Thread {
        val DAPThread = Thread()
        DAPThread.id = JBThread.getId().toInt()
        return DAPThread
    }

    fun breakpointJBFromDAP(DAPBreakpoint: Breakpoint): LLBreakpoint {
        val source = DAPBreakpoint.source
        var sourcePath = if (source == null) "" else Objects.requireNonNullElseGet(
            source.path
        ) { Objects.requireNonNullElse(source.origin, "unknown") }
        sourcePath = toJBPath(sourcePath)
        return LLBreakpoint(
            DAPBreakpoint.id,
            sourcePath,
            Objects.requireNonNullElse<Int>(DAPBreakpoint.line, 0) - 1,
            null
        )
    }

    fun getLocation(DAPBreakpoint: Breakpoint): LLBreakpointLocation? {
        val ref = DAPBreakpoint.instructionReference ?: return null
        val addr = ref.substring(2).toLong(16)
        var fl: FileLocation? = null
        val src = DAPBreakpoint.source
        if (src != null) {
            fl = FileLocation(src.path, DAPBreakpoint.line)
        }
        return LLBreakpointLocation(DAPBreakpoint.id.toString() + "", Address.fromUnsignedLong(addr), fl)
    }

    fun breakpointDAPFromJB(JBBreakpoint: LLBreakpoint): Breakpoint {
        val DAPBreakpoint = Breakpoint()
        DAPBreakpoint.id = JBBreakpoint.getId()
        DAPBreakpoint.line = JBBreakpoint.getOrigLine() + 1
        val source = Source()
        source.path = JBBreakpoint.getOrigFile()
        DAPBreakpoint.source = source
        DAPBreakpoint.message = JBBreakpoint.getCondition()
        return DAPBreakpoint
    }

    fun moduleJBFromDAP(DAPModule: Module): LLModule {
        return LLModule(toJBPath(DAPModule.path))
    }

    fun moduleDAPFromJB(JBModule: LLModule): Module {
        val DAPModule = Module()
        DAPModule.path = toJBPath(JBModule.path)
        DAPModule.name = JBModule.name
        return DAPModule
    }

    fun frameJBFromDAP(
        DAPFrame: StackFrame,
        helperBreakpoint: DAPDriver.MappedBreakpoint?,
        modules: Map<Int, DAPDriver.MappedModule>
    ): LLFrame {
        val ptr = parseAddress(DAPFrame.instructionPointerReference)
        val name = DAPFrame.name
        val inline = name.startsWith("[Inline Frame] ")
        val function = name.substring(name.indexOf('!') + 1, name.indexOf('('))
        val moduleID = DAPFrame.moduleId
        var moduleName: String? = null
        if (moduleID != null) {
            if (moduleID.isRight) {
                moduleName = moduleID.right
            } else {
                val module = modules[moduleID.left]!!
                moduleName = module.java.name
            }
        }
        var line = DAPFrame.line
        var sourcePath: String?
        run {
            val src = DAPFrame.source
            sourcePath = if (src == null) null else toJBPath(src.path)
        }
        if (helperBreakpoint != null) {
            if (line == 0) {
                line = helperBreakpoint.dap.line
            }
            if (sourcePath == null) {
                val src = helperBreakpoint.dap.source
                if (src != null) {
                    sourcePath = toJBPath(src.path)
                }
            }
        }
        return LLFrame(
            DAPFrame.id,
            function,
            sourcePath,
            null,
            line - 1,
            ptr,
            ZigDebuggerLanguage,
            false,
            inline,
            moduleName
        )
    }

    fun toSource(path: String): Source {
        val src = Source()
        val absolute = Path.of(path).toAbsolutePath()
        src.name = absolute.fileName.toString()
        src.path = toWinPath(absolute.toString())
        return src
    }

    fun toWinPath(path: String): String {
        return path.replace('/', '\\')
    }

    fun toJBPath(path: String): String {
        return path.replace('\\', '/')
    }

    fun parseAddressNullable(address: String?): Long? {
        if (address == null) return null
        return parseAddress(address)
    }

    fun parseAddress(address: String?): Long {
        if (address == null) return 0L
        if (!address.startsWith("0x")) return java.lang.Long.parseUnsignedLong(address)
        return java.lang.Long.parseUnsignedLong(address.substring(2), 16)
    }

    fun stringifyAddress(address: Long): String {
        return "0x" + java.lang.Long.toHexString(address)
    }

    private val HEX_FIX_REGEX: Pattern = Pattern.compile("([0-9A-F]+)(?<!\\W)h")
    suspend fun instructionJBFromDAP(
        DAPInstruction: DisassembledInstruction,
        loc: Source?,
        startLineIn: Int?,
        endLineIn: Int?,
        uniq: Boolean,
        symbol: LLSymbolOffset?
    ): LLInstruction {
        var startLine = startLineIn
        var endLine = endLineIn
        val address: Address = Address.parseHexString(DAPInstruction.address)
        val byteStrings =
            DAPInstruction.instructionBytes.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val bytes = ArrayList<Byte>(byteStrings.size)
        for (byteString in byteStrings) {
            bytes.add(byteString.toInt(16).toByte())
        }
        var comment: String? = null
        if (loc != null && startLine != null && endLine != null && uniq) run {
            val pathStr = toJBPath(loc.path)
            val path: Path
            try {
                path = Path.of(pathStr)
            } catch (ignored: InvalidPathException) {
                return@run
            }
            val text = readAction {
                val file = VfsUtil.findFile(path, true) ?: return@readAction null
                val doc: Document = file.findDocument() ?: return@readAction null
                doc.immutableCharSequence.toString().split("(\r\n|\r|\n)".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            }
            if (text == null) return@run
            startLine -= 1
            endLine -= 1
            if (text.size <= endLine) return@run
            comment = text[endLine]
        }
        val nicerDisassembly = StringBuilder()
        val disassembly = DAPInstruction.instruction
        val matcher = HEX_FIX_REGEX.matcher(disassembly)
        var prevEnd = 0
        while (matcher.find()) {
            nicerDisassembly.append(disassembly, prevEnd, matcher.start())
            val hex = matcher.group(1).lowercase(Locale.getDefault())
            nicerDisassembly.append("0x").append(hex)
            prevEnd = matcher.end()
        }
        if (prevEnd < disassembly.length) nicerDisassembly.append(disassembly, prevEnd, disassembly.length)
        return LLInstruction.create(
            address,
            bytes,
            nicerDisassembly.toString(),
            comment,
            symbol
        )
    }

    fun memoryJBFromDAP(DAPMemory: ReadMemoryResponse): LLMemoryHunk {
        val address = parseAddress(DAPMemory.address)
        val bytes = Base64.getDecoder().decode(DAPMemory.data)
        val range = AddressRange(
            Address.fromUnsignedLong(address),
            Address.fromUnsignedLong(address + bytes.size - 1)
        )
        return LLMemoryHunk(range, bytes)
    }

    @Throws(com.intellij.execution.ExecutionException::class)
    fun <T> get(future: CompletableFuture<T>): T {
        try {
            return future[4, TimeUnit.SECONDS]
        } catch (e: InterruptedException) {
            throw com.intellij.execution.ExecutionException(e)
        } catch (e: TimeoutException) {
            throw com.intellij.execution.ExecutionException(e)
        } catch (e: ExecutionException) {
            throw com.intellij.execution.ExecutionException(e.cause)
        }
    }
}
