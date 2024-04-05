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

package com.falsepattern.zigbrains.debugger.runner.build;

import com.falsepattern.zigbrains.debugger.runner.base.ZigDebugParametersBase;
import com.falsepattern.zigbrains.project.execution.build.ProfileStateBuild;
import com.falsepattern.zigbrains.project.toolchain.AbstractZigToolchain;
import com.falsepattern.zigbrains.project.util.CLIUtil;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.jetbrains.cidr.execution.Installer;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration;
import lombok.Cleanup;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class ZigDebugParametersBuild extends ZigDebugParametersBase<ProfileStateBuild> {
    private static final String BoilerplateNotice = "\nPlease edit this intellij build configuration and specify the path of the executable created by \"zig build\" directly!";
    public ZigDebugParametersBuild(DebuggerDriverConfiguration driverConfiguration, AbstractZigToolchain toolchain, ProfileStateBuild profileStateBuild) {
        super(driverConfiguration, toolchain, profileStateBuild);
    }

    @Override
    public @NotNull Installer getInstaller() {
        return new Installer() {
            private File executableFile;
            @Override
            public @NotNull GeneralCommandLine install() throws ExecutionException {
                val commandLine = profileState.getCommandLine(toolchain, true);
                val outputOpt = CLIUtil.execute(commandLine, Integer.MAX_VALUE);
                if (outputOpt.isEmpty()) {
                    throw new ExecutionException("Failed to execute \"zig " + commandLine.getParametersList().getParametersString() + "\"!");
                }
                val output = outputOpt.get();
                if (output.getExitCode() != 0) {
                    throw new ExecutionException("Zig compilation failed with exit code " + output.getExitCode() + "\nError output:\n" + output.getStdout() + "\n" + output.getStderr());
                }

                val cfg = profileState.configuration();
                val workingDir = cfg.getWorkingDirectory().getPath().orElse(null);
                val exePath = profileState.configuration().getExePath().getPath();
                Path exe;
                if (exePath.isEmpty()) {
                    //Attempt autodetect, should work for trivial cases, and make basic users happy, while advanced
                    // users can use manual executable paths.
                    if (workingDir == null) {
                        throw new ExecutionException("Cannot find working directory to run debugged executable!" + BoilerplateNotice);
                    }
                    val expectedOutputDir = workingDir.resolve(Path.of("zig-out", "bin"));
                    if (!Files.exists(expectedOutputDir)) {
                        throw new ExecutionException("Could not auto-detect default executable output directory \"zig-out/bin\"!" + BoilerplateNotice);
                    }
                    try (val filesInOutput = Files.list(expectedOutputDir)) {
                        val executables = filesInOutput.filter(Files::isRegularFile).filter(Files::isExecutable).toList();
                        if (executables.size() > 1) {
                            throw new ExecutionException("Multiple executables found!" + BoilerplateNotice);
                        }
                        exe = executables.get(0);
                    } catch (IOException e) {
                        throw new ExecutionException("Could not scan output directory \"" + expectedOutputDir + "\"!" + BoilerplateNotice);
                    }
                } else {
                    exe = exePath.get();
                }

                if (!Files.exists(exe)) {
                    throw new ExecutionException("File " + exe + " does not exist!");
                } else if (!Files.isExecutable(exe)) {
                    throw new ExecutionException("File " + exe + " is not executable!");
                }

                executableFile = exe.toFile();

                //Construct new command line
                val cli = new GeneralCommandLine().withExePath(executableFile.getAbsolutePath());
                cfg.getWorkingDirectory().getPath().ifPresent(x -> cli.setWorkDirectory(x.toFile()));
                cli.addParameters(cfg.getExeArgs().args);
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
