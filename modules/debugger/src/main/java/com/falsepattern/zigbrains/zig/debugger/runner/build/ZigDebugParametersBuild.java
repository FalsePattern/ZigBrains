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

package com.falsepattern.zigbrains.zig.debugger.runner.build;

import com.falsepattern.zigbrains.project.execution.build.ProfileStateBuild;
import com.falsepattern.zigbrains.project.toolchain.AbstractZigToolchain;
import com.falsepattern.zigbrains.project.util.CLIUtil;
import com.falsepattern.zigbrains.zig.debugger.runner.base.ZigDebugParametersBase;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.jetbrains.cidr.execution.Installer;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ZigDebugParametersBuild extends ZigDebugParametersBase<ProfileStateBuild> {
    public ZigDebugParametersBuild(DebuggerDriverConfiguration driverConfiguration, AbstractZigToolchain toolchain, ProfileStateBuild profileStateBuild) {
        super(driverConfiguration, toolchain, profileStateBuild);
    }

    @Override
    public @NotNull Installer getInstaller() {
        return new Installer() {
            private File executableFile;
            @Override
            public @NotNull GeneralCommandLine install() throws ExecutionException {
                val exePath = profileState.configuration().getExePath().getPath();
                if (exePath.isEmpty()) {
                    throw new ExecutionException("Please specify the output exe path to debug \"zig build\" tasks!");
                }
                Path exe = exePath.get();
                val commandLine = profileState.getCommandLine(toolchain, true);
                val outputOpt = CLIUtil.execute(commandLine, Integer.MAX_VALUE);
                if (outputOpt.isEmpty()) {
                    throw new ExecutionException("Failed to execute \"zig " + commandLine.getParametersList().getParametersString() + "\"!");
                }
                val output = outputOpt.get();
                if (output.getExitCode() != 0) {
                    throw new ExecutionException("Zig compilation failed with exit code " + output.getExitCode() + "\nError output:\n" + output.getStdout() + "\n" + output.getStderr());
                }

                if (!Files.exists(exe) || !Files.isExecutable(exe)) {
                    throw new ExecutionException("File " + exe + " does not exist or is not executable!");
                }

                executableFile = exe.toFile();

                //Construct new command line
                val cfg = profileState.configuration();
                val cli = new GeneralCommandLine().withExePath(executableFile.getAbsolutePath());
                cfg.getWorkingDirectory().getPath().ifPresent(x -> cli.setWorkDirectory(x.toFile()));
                cli.withCharset(StandardCharsets.UTF_8);
                cli.withRedirectErrorStream(true);
                return cli;
            }

            @Override
            public @NotNull File getExecutableFile() {
                return executableFile;
            }
        };
    }
}
