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

package com.falsepattern.zigbrains.debugger.win;

import com.falsepattern.zigbrains.debugger.dap.DAPDebuggerDriverConfiguration;
import com.falsepattern.zigbrains.debugger.dap.DAPDriver;
import com.falsepattern.zigbrains.debugger.dap.WrappedDebugServer;
import com.intellij.execution.ExecutionException;
import com.intellij.util.system.CpuArch;
import com.jetbrains.cidr.ArchitectureType;
import lombok.Cleanup;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.eclipse.lsp4j.debug.Capabilities;
import org.eclipse.lsp4j.debug.OutputEventArguments;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;
import org.eclipse.lsp4j.jsonrpc.JsonRpcException;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.jsonrpc.MessageIssueException;
import org.eclipse.lsp4j.jsonrpc.debug.messages.DebugResponseMessage;
import org.eclipse.lsp4j.jsonrpc.messages.Message;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.zip.Inflater;

public class WinDAPDriver extends DAPDriver<
        IDebugProtocolServer, WrappedDebugServer<IDebugProtocolServer>,
        WinDAPDriver.WinDAPDebuggerClient
        > {
    private final CompletableFuture<HandshakeResponse> handshakeFuture = new CompletableFuture<>();
    public WinDAPDriver(@NotNull Handler handler, DAPDebuggerDriverConfiguration config) throws ExecutionException {
        super(handler, config);
        DAPDriver$postConstructor();
    }

    @Override
    public void DAPDriver$postConstructor$invoke() {

    }

    @Override
    protected MessageConsumer wrapMessageConsumer(MessageConsumer messageConsumer) {
        return new MessageConsumer() {
            private boolean verifyHandshake = true;
            @Override
            public void consume(Message message) throws MessageIssueException, JsonRpcException {
                if (verifyHandshake && message instanceof DebugResponseMessage res && res.getMethod().equals("handshake")) {
                    verifyHandshake = false;
                    res.setResponseId(1);
                }
                messageConsumer.consume(message);
            }
        };
    }

    @Override
    protected Class<IDebugProtocolServer> getServerInterface() {
        return IDebugProtocolServer.class;
    }

    @Override
    protected WrappedDebugServer<IDebugProtocolServer> wrapDebugServer(IDebugProtocolServer remoteProxy) {
        return new WrappedDebugServer<>(remoteProxy);
    }

    @Override
    protected WinDAPDebuggerClient createDebuggerClient() {
        return this.new WinDAPDebuggerClient();
    }

    @Override
    protected CompletableFuture<?> wrapInitialize(CompletableFuture<Capabilities> capabilitiesCompletableFuture) {
        return capabilitiesCompletableFuture.thenCombine(handshakeFuture, (res, hs) -> res);
    }

    //Weird nested generics interaction, not suppressing unchecked causes a linter error, I have no idea how to fix this
    @SuppressWarnings({"unchecked", "RedundantSuppression"})
    protected class WinDAPDebuggerClient extends DAPDebuggerClient {
        @Override
        public void output(OutputEventArguments args) {
            if ("telemetry".equals(args.getCategory())) {
                return;
            }
            super.output(args);
        }

        @JsonRequest
        public CompletableFuture<HandshakeResponse> handshake(HandshakeRequest handshake) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    val hasher = MessageDigest.getInstance("SHA-256");
                    hasher.update(handshake.getValue().getBytes(StandardCharsets.UTF_8));
                    var inflater = new Inflater(true);
                    @Cleanup val coconutJpg = DAPDebuggerClient.class.getResourceAsStream("/coconut.jpg");
                    if (coconutJpg == null) {
                        throw new RuntimeException("No coconut");
                    }
                    val coconut = coconutJpg.readAllBytes();
                    inflater.setInput(coconut, coconut.length - 80, 77);
                    inflater.finished();
                    var b = new byte[1];
                    while (inflater.inflate(b) > 0) {
                        hasher.update(b);
                    }
                    return new HandshakeResponse(new String(coconut, coconut.length - 3, 3) + Base64.getEncoder().encodeToString(hasher.digest()));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).thenApply(handshakeResponse -> {
                handshakeFuture.complete(handshakeResponse);
                return handshakeResponse;
            });
        }
    }

    @Override
    public @Nullable String getArchitecture() throws ExecutionException {
        return ArchitectureType.forVmCpuArch(CpuArch.CURRENT).getId();
    }

    @Data
    @NoArgsConstructor
    public static class HandshakeRequest {
        @NonNull
        private String value;

        @Override
        public String toString() {
            val b = new ToStringBuilder(this);
            b.add("value", this.value);
            return b.toString();
        }
    }

    @Data
    @NoArgsConstructor
    @RequiredArgsConstructor
    public static class HandshakeResponse {
        @NonNull
        private String signature;

        @Override
        public String toString() {
            val b = new ToStringBuilder(this);
            b.add("signature", this.signature);
            return b.toString();
        }
    }
}
