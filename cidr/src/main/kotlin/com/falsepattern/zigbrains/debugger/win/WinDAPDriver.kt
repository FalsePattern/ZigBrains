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

package com.falsepattern.zigbrains.debugger.win

import com.falsepattern.zigbrains.debugger.dap.DAPDriver
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.util.system.CpuArch
import com.jetbrains.cidr.ArchitectureType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.sync.Semaphore
import org.eclipse.lsp4j.debug.Capabilities
import org.eclipse.lsp4j.debug.OutputEventArguments
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer
import org.eclipse.lsp4j.debug.util.ToStringBuilder
import org.eclipse.lsp4j.jsonrpc.MessageConsumer
import org.eclipse.lsp4j.jsonrpc.debug.messages.DebugResponseMessage
import org.eclipse.lsp4j.jsonrpc.messages.Message
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest
import java.io.InputStream
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.zip.Inflater

class WinDAPDriver(handler: Handler) : DAPDriver<IDebugProtocolServer, WinDAPDriver.WinDAPDebuggerClient>(handler) {
    private val handshakeFinished = Semaphore(1, 1)

    override fun createDebuggerClient(): WinDAPDebuggerClient {
        return WinDAPDebuggerClient()
    }

    override fun getServerInterface(): Class<IDebugProtocolServer> {
        return IDebugProtocolServer::class.java
    }

    override fun wrapMessageConsumer(mc: MessageConsumer): MessageConsumer {
        return object: MessageConsumer {
            private var verifyHandshake = true
            override fun consume(message: Message) {
                if (verifyHandshake && message is DebugResponseMessage && message.method == "handshake") {
                    verifyHandshake = false
                    message.setResponseId(1)
                }
                mc.consume(message)
            }

        }
    }

    override suspend fun postInitialize(capabilities: Capabilities) {
        handshakeFinished.acquire()
    }

    inner class WinDAPDebuggerClient: DAPDriver<IDebugProtocolServer, WinDAPDebuggerClient>.DAPDebuggerClient() {
        override fun output(args: OutputEventArguments) {
            if ("telemetry" == args.category)
                return

            super.output(args)
        }

        @JsonRequest
        fun handshake(handshake: HandshakeRequest): CompletableFuture<HandshakeResponse> {
            return zigCoroutineScope.async(Dispatchers.IO) {
                handshakeSuspend(handshake)
            }.asCompletableFuture()
        }

        private fun handshakeSuspend(handshake: HandshakeRequest): HandshakeResponse {
            val hasher = MessageDigest.getInstance("SHA-256")
            hasher.update(handshake.value.encodeToByteArray())
            val inflater = Inflater(true)
            val coconut = DAPDebuggerClient::class.java.getResourceAsStream("/coconut.jpg")?.use(InputStream::readAllBytes) ?: throw RuntimeException("No coconut")
            inflater.setInput(coconut, coconut.size - 80, 77)
            inflater.finished()
            val b = ByteArray(1)
            while (inflater.inflate(b) > 0)
                hasher.update(b)
            val result = HandshakeResponse(String(coconut, coconut.size - 3, 3) + Base64.getEncoder().encodeToString(hasher.digest()))
            handshakeFinished.release()
            return result
        }
    }

    override fun getArchitecture(): String {
        return ArchitectureType.forVmCpuArch(CpuArch.CURRENT).id
    }

    data class HandshakeRequest(var value: String) {
        constructor() : this("")
        override fun toString(): String {
            val b = ToStringBuilder(this)
            b.add("value", value)
            return b.toString()
        }
    }

    data class HandshakeResponse(var signature: String) {
        constructor() : this("")

        override fun toString(): String {
            val b = ToStringBuilder(this)
            b.add("signature", this.signature)
            return b.toString()
        }
    }
}