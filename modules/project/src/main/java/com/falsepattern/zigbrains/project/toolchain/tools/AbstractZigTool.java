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
import com.falsepattern.zigbrains.project.util.CLIUtil;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.util.UserDataHolder;
import kotlin.text.Charsets;
import lombok.AllArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@AllArgsConstructor
public abstract class AbstractZigTool {
    public final AbstractZigToolchain toolchain;
    public String toolName;

    public Path executable() {
        return toolchain.pathToExecutable(toolName);
    }

    public final Optional<ProcessOutput> callWithArgs(@Nullable Path workingDirectory, int timeoutMillis, @NotNull UserDataHolder data, String @NotNull... parameters) {
        return CLIUtil.execute(createBaseCommandLine(workingDirectory, data, parameters),
                               timeoutMillis);
    }

    protected final GeneralCommandLine createBaseCommandLine(@Nullable Path workingDirectory,
                                                             @NotNull UserDataHolder data,
                                                             String @NotNull... parameters) {
        return createBaseCommandLine(workingDirectory, Collections.emptyMap(), data, parameters);
    }

    protected final GeneralCommandLine createBaseCommandLine(@Nullable Path workingDirectory,
                                                             @NotNull Map<String, String> environment,
                                                             @NotNull UserDataHolder data,
                                                             String @NotNull... parameters) {
        return createBaseCommandLine(workingDirectory, environment, data, List.of(parameters));
    }

    protected final GeneralCommandLine createBaseCommandLine(@Nullable Path workingDirectory,
                                                             @NotNull UserDataHolder data,
                                                             @NotNull List<String> parameters) {
        return createBaseCommandLine(workingDirectory, Collections.emptyMap(), data, parameters);
    }

    protected GeneralCommandLine createBaseCommandLine(@Nullable Path workingDirectory,
                                                       @NotNull Map<String, String> environment,
                                                       @NotNull UserDataHolder data,
                                                       @NotNull List<String> parameters) {
        val cli = new GeneralCommandLine(executable().toString())
                .withWorkDirectory(workingDirectory == null ? null : workingDirectory.toString())
                .withParameters(parameters)
                .withEnvironment(environment)
                .withCharset(Charsets.UTF_8);
        return toolchain.patchCommandLine(cli, data);
    }
}
