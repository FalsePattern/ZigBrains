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

package com.falsepattern.zigbrains.debugger;

import com.falsepattern.zigbrains.debugger.runner.base.PreLaunchAware;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.openapi.util.Expirable;
import com.intellij.xdebugger.XDebugSession;
import com.jetbrains.cidr.execution.RunParameters;
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess;
import com.jetbrains.cidr.execution.debugger.CidrLocalDebugProcess;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class ZigLocalDebugProcess extends CidrLocalDebugProcess {
    private final ReentrantLock lock = new ReentrantLock();
    public enum SuppressionLevel {
        PreStart,
        PostStart,
        Unsuppressed
    }
    private volatile SuppressionLevel suppressionLevel;
    private final List<Runnable> preStart = new ArrayList<>();
    private final List<Runnable> postStart = new ArrayList<>();
    public ZigLocalDebugProcess(@NotNull RunParameters parameters, @NotNull XDebugSession session, @NotNull TextConsoleBuilder consoleBuilder)
            throws ExecutionException {
        super(parameters, session, consoleBuilder, (project) -> Filter.EMPTY_ARRAY, false);
        suppressionLevel = parameters instanceof PreLaunchAware ? SuppressionLevel.PreStart : SuppressionLevel.Unsuppressed;
    }

    public void doStart() {
        start();
        lock.lock();
        try {
            if (suppressionLevel == SuppressionLevel.PreStart)
                suppressionLevel = SuppressionLevel.PostStart;
        } finally {
            lock.unlock();
        }
    }

    public void unSuppress(boolean runPreStart) {
        lock.lock();
        try {
            suppressionLevel = SuppressionLevel.Unsuppressed;
            if (runPreStart) {
                for (val r: preStart)
                    r.run();
            }
            for (val r: postStart)
                r.run();
            preStart.clear();
            postStart.clear();
        } finally {
            lock.unlock();
        }
    }

    private <T> CompletableFuture<T> suppressedFuture(Supplier<CompletableFuture<T>> original) {
        if (suppressionLevel == SuppressionLevel.Unsuppressed)
            return original.get();

        lock.lock();
        try {
            val level = suppressionLevel;
            if (level == SuppressionLevel.Unsuppressed)
                return original.get();

            val bypass = new CompletableFuture<T>();
            val task = (Runnable)() ->
                    original.get()
                            .thenAccept(bypass::complete)
                            .exceptionally((ex) -> {
                                bypass.completeExceptionally(ex);
                                return null;
                            });
            switch (level) {
                case PreStart -> preStart.add(task);
                case PostStart -> postStart.add(task);
            }
            return bypass;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public @NotNull CompletableFuture<Void> postCommand(@NotNull CidrDebugProcess.VoidDebuggerCommand command) {
        return suppressedFuture(() -> super.postCommand(command));
    }

    @Override
    public @NotNull CompletableFuture<Void> postCommand(
            @Nullable Expirable expirable, @NotNull CidrDebugProcess.VoidDebuggerCommand command) {
        return suppressedFuture(() -> super.postCommand(expirable, command));
    }

    @Override
    public @NotNull CompletableFuture<Void> postCommand(
            @NotNull CidrDebugProcess.VoidDebuggerCommand command, boolean useAlternativeDispatcher) {
        return suppressedFuture(() -> super.postCommand(command, useAlternativeDispatcher));
    }

    @Override
    public @NotNull <T> CompletableFuture<T> postCommand(@NotNull CidrDebugProcess.DebuggerCommand<T> command) {
        return suppressedFuture(() -> super.postCommand(command));
    }

    @Override
    public @NotNull <T> CompletableFuture<T> postCommand(
            @Nullable Expirable expirable, @NotNull CidrDebugProcess.DebuggerCommand<T> command) {
        return suppressedFuture(() -> super.postCommand(expirable, command));
    }

    @Override
    public @NotNull <T> CompletableFuture<T> postCommand(
            @NotNull CidrDebugProcess.DebuggerCommand<T> command, boolean useAlternativeDispatcher) {
        return suppressedFuture(() -> super.postCommand(command, useAlternativeDispatcher));
    }
}
