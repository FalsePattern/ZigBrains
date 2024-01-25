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

import com.falsepattern.zigbrains.project.toolchain.flavours.AbstractZigToolchainFlavour;
import com.falsepattern.zigbrains.project.toolchain.tools.ZigCompilerTool;
import com.intellij.execution.configurations.GeneralCommandLine;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Objects;

@RequiredArgsConstructor
public abstract class AbstractZigToolchain {
    public final Path location;

    public static @Nullable AbstractZigToolchain suggest() {
        return suggest(null);
    }

    public static @Nullable AbstractZigToolchain suggest(@Nullable Path projectDir) {
        return AbstractZigToolchainFlavour.getApplicableFlavours()
                                          .stream()
                                          .flatMap(it -> it.suggestHomePaths().stream())
                                          .filter(Objects::nonNull)
                                          .map(it -> ZigToolchainProvider.findToolchain(it.toAbsolutePath()))
                                          .filter(Objects::nonNull)
                                          .findFirst().orElse(null);
    }

    public ZigCompilerTool zig() {
        return new ZigCompilerTool(this);
    }

    public abstract int executionTimeoutInMilliseconds();

    public abstract GeneralCommandLine patchCommandLine(GeneralCommandLine commandLine);

    public abstract Path pathToExecutable(String toolName);

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AbstractZigToolchain azt) {
            return Objects.equals(location, azt.location);
        }
        return false;
    }
}
