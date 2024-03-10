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

package com.falsepattern.zigbrains.zig.debugger.runner.binary;

import com.falsepattern.zigbrains.project.execution.binary.ProfileStateBinary;
import com.falsepattern.zigbrains.project.toolchain.AbstractZigToolchain;
import com.falsepattern.zigbrains.zig.debugger.runner.base.ZigDebugParametersBase;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.jetbrains.cidr.execution.Installer;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class ZigDebugParametersBinary extends ZigDebugParametersBase<ProfileStateBinary> {
    public ZigDebugParametersBinary(DebuggerDriverConfiguration driverConfiguration, AbstractZigToolchain toolchain, ProfileStateBinary profileStateBinary) {
        super(driverConfiguration, toolchain, profileStateBinary);
    }

    @Override
    public @NotNull Installer getInstaller() {
        return new Installer() {
            private File executableFile;
            @Override
            public @NotNull GeneralCommandLine install() throws ExecutionException {
                val cli = profileState.getCommandLine(toolchain, true);
                executableFile = profileState.configuration().getExePath().getPathOrThrow().toFile();
                return cli;
            }

            @Override
            public @NotNull File getExecutableFile() {
                return executableFile;
            }
        };
    }
}
