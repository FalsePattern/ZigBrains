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

package com.falsepattern.zigbrains.project.toolchain;

import com.falsepattern.zigbrains.common.util.PathUtil;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;

import java.nio.file.Path;

public class LocalZigToolchain extends AbstractZigToolchain{
    public LocalZigToolchain(Path location) {
        super(location);
    }

    @Override
    public int executionTimeoutInMilliseconds() {
        return 1000;
    }

    @Override
    public GeneralCommandLine patchCommandLine(GeneralCommandLine commandLine) {
        return commandLine;
    }

    @Override
    public Path pathToExecutable(String toolName) {
        return PathUtil.pathToExecutable(getLocation(), toolName);
    }

    public static LocalZigToolchain ensureLocal(AbstractZigToolchain toolchain) throws ExecutionException {
        if (!(toolchain instanceof LocalZigToolchain $toolchain)) {
            throw new ExecutionException("The debugger only supports local zig toolchains!");
        }
        return $toolchain;
    }
}
