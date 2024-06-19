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

package com.falsepattern.zigbrains.clion;

import com.falsepattern.zigbrains.debugbridge.ZigDebuggerDriverConfigurationProvider;
import com.falsepattern.zigbrains.debugger.settings.ZigDebuggerSettings;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.jetbrains.cidr.cpp.execution.debugger.backend.CLionGDBDriverConfiguration;
import com.jetbrains.cidr.cpp.execution.debugger.backend.CLionLLDBDriverConfiguration;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration;
import lombok.val;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class ZigClionDebuggerDriverConfigurationProvider implements ZigDebuggerDriverConfigurationProvider {
    private static final Logger LOG = Logger.getInstance(ZigClionDebuggerDriverConfigurationProvider.class);

    @Override
    public @Nullable Supplier<DebuggerDriverConfiguration> getDebuggerConfiguration(Project project, boolean isElevated, boolean emulateTerminal) {
        if (SystemInfo.isWindows)
            return null;

        if (!ZigDebuggerSettings.getInstance().useClion)
            return null;

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

        CPPToolchains.Toolchain finalToolchain = toolchain;
        return switch (toolchain.getDebuggerKind()) {
            case CUSTOM_GDB, BUNDLED_GDB -> () -> new CLionGDBDriverConfiguration(project, finalToolchain);
            case BUNDLED_LLDB -> () -> new CLionLLDBDriverConfiguration(project, finalToolchain);
        };
    }
}
