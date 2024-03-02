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

import com.falsepattern.zigbrains.project.toolchain.AbstractZigToolchain;
import com.falsepattern.zigbrains.project.toolchain.ZigToolchainEnvironmentSerializable;
import com.falsepattern.zigbrains.project.util.CLIUtil;
import com.google.gson.Gson;
import com.intellij.execution.process.ProcessOutput;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Optional;

public class ZigCompilerTool extends AbstractZigTool{
    public static final String TOOL_NAME = "zig";

    public ZigCompilerTool(AbstractZigToolchain toolchain) {
        super(toolchain, TOOL_NAME);
    }

    public Optional<ZigToolchainEnvironmentSerializable> getEnv(@Nullable Path workingDirectory) {
        return callWithArgs(workingDirectory, toolchain.executionTimeoutInMilliseconds(), "env")
                .map(ProcessOutput::getStdoutLines)
                .map(lines -> new Gson().fromJson(String.join(" ", lines), ZigToolchainEnvironmentSerializable.class));

    }

    public Optional<String> getStdPath(@Nullable Path workingDirectory) {
        return getEnv(workingDirectory).map(ZigToolchainEnvironmentSerializable::stdDirectory);
    }

    public Optional<String> queryVersion(@Nullable Path workingDirectory) {
        return getEnv(workingDirectory).map(ZigToolchainEnvironmentSerializable::version);
    }
}
