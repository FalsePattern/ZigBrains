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

package com.falsepattern.zigbrains.project.toolchain.tools;

import com.falsepattern.zigbrains.common.util.Lazy;
import com.falsepattern.zigbrains.project.toolchain.AbstractZigToolchain;
import com.falsepattern.zigbrains.project.toolchain.ZigToolchainEnvironmentSerializable;
import com.google.gson.Gson;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import lombok.val;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class ZigCompilerTool extends AbstractZigTool{
    public static final String TOOL_NAME = "zig";
    private final Lazy<Optional<String>> version;
    private final Lazy<Optional<Path>> stdPath;

    public ZigCompilerTool(AbstractZigToolchain toolchain) {
        super(toolchain, TOOL_NAME);
        val app = ApplicationManager.getApplication();
        val baseFuture = app.executeOnPooledThread(() -> getEnv());
        version = new Lazy<>(() -> {
            try {
                return baseFuture.get().map(ZigToolchainEnvironmentSerializable::version);
            } catch (InterruptedException | ExecutionException e) {
                return Optional.empty();
            }
        });
        stdPath = new Lazy<>(() -> {
            try {
                return baseFuture.get().map(ZigToolchainEnvironmentSerializable::stdDirectory)
                        .map(pathStr -> {
                            try {
                                val path = Path.of(pathStr);
                                if (path.isAbsolute()) {
                                    return path;
                                }
                                val resolvedPath = toolchain.getLocation().resolve(path);
                                if (resolvedPath.isAbsolute()) {
                                    return resolvedPath;
                                }
                                return null;
                            } catch (InvalidPathException e) {
                                return null;
                            }
                        });
            } catch (InterruptedException | ExecutionException e) {
                return Optional.empty();
            }
        });
    }

    public Optional<ZigToolchainEnvironmentSerializable> getEnv() {
        return callWithArgs(toolchain.getLocation(), toolchain.executionTimeoutInMilliseconds(), toolchain.getDataForSelfRuns(), "env")
                .map(ProcessOutput::getStdoutLines)
                .map(lines -> new Gson().fromJson(String.join(" ", lines), ZigToolchainEnvironmentSerializable.class));

    }

    public Optional<Path> getStdPath() {
        return stdPath.get();
    }

    public Optional<String> queryVersion() {
        return version.get();
    }
}
