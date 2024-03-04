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

package com.falsepattern.zigbrains.cpp;

import com.falsepattern.zigbrains.debugbridge.DebuggerDriverProvider;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.jetbrains.cidr.cpp.execution.debugger.backend.CLionGDBDriverConfiguration;
import com.jetbrains.cidr.cpp.execution.debugger.backend.CLionLLDBDriverConfiguration;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration;
import lombok.val;
import org.jetbrains.annotations.Nullable;

public class CPPDebuggerDriverProvider implements DebuggerDriverProvider {
    private static final Logger LOG = Logger.getInstance(CPPDebuggerDriverProvider.class);
    @Override
    public @Nullable DebuggerDriverConfiguration getDebuggerConfiguration(Project project) {
        val toolchains = CPPToolchains.getInstance();
        var toolchain = toolchains.getToolchainByNameOrDefault("Zig");
        if (toolchain == null || !toolchain.isDebuggerSupported()) {
            LOG.info("Couldn't find debug-compatible C++ toolchain with name \"Zig\"");
            toolchain = toolchains.getDefaultToolchain();
        }
        if (toolchain == null || !toolchain.isDebuggerSupported()) {
            LOG.info("Couldn't find debug-compatible C++ default toolchain");
            return null;
        }

        return switch (toolchain.getDebuggerKind()) {
            case CUSTOM_GDB, BUNDLED_GDB -> new CLionGDBDriverConfiguration(project, toolchain);
            case BUNDLED_LLDB -> new CLionLLDBDriverConfiguration(project, toolchain);
        };
    }
}
