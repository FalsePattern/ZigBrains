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

import com.falsepattern.zigbrains.debugger.runner.base.PreLaunchAware;
import com.falsepattern.zigbrains.debugger.runner.base.PreLaunchProcessListener;
import com.falsepattern.zigbrains.debugger.runner.base.ZigDebugEmitBinaryInstaller;
import com.falsepattern.zigbrains.debugger.runner.base.ZigDebugParametersBase;
import com.falsepattern.zigbrains.project.execution.build.ProfileStateBuild;
import com.falsepattern.zigbrains.project.toolchain.AbstractZigToolchain;
import com.intellij.execution.ExecutionException;
import com.intellij.openapi.util.SystemInfo;
import com.jetbrains.cidr.execution.Installer;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class ZigDebugParametersBuild extends ZigDebugParametersBase<ProfileStateBuild> implements PreLaunchAware {
    private static final String BoilerplateNotice = "\nPlease edit this intellij build configuration and specify the path of the executable created by \"zig build\" directly!";

    private volatile File executableFile;

    public ZigDebugParametersBuild(DebuggerDriverConfiguration driverConfiguration, AbstractZigToolchain toolchain, ProfileStateBuild profileStateBuild) {
        super(driverConfiguration, toolchain, profileStateBuild);

    }

    private File compileExe(PreLaunchProcessListener listener) throws ExecutionException {
        val commandLine = profileState.getCommandLine(toolchain, true);
        if (listener.executeCommandLineWithHook(commandLine))
            return null;
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
                exe = getExe(filesInOutput);
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

        return exe.toFile();
    }

    private static @NotNull Path getExe(Stream<Path> files) throws ExecutionException {
        files = files.filter(Files::isRegularFile);
        if (SystemInfo.isWindows) {
            files = files.filter(file -> file.getFileName().toString().endsWith(".exe"));
        } else {
            files = files.filter(Files::isExecutable);
        }
        val executables = files.toList();
        if (executables.size() > 1) {
            throw new ExecutionException("Multiple executables found!" + BoilerplateNotice);
        }
        return executables.get(0);
    }

    @Override
    public void preLaunch(PreLaunchProcessListener listener) throws ExecutionException {
        this.executableFile = compileExe(listener);
    }

    @Override
    public @NotNull Installer getInstaller() {
        assert executableFile != null;
        return new ZigDebugEmitBinaryInstaller<>(profileState, toolchain, executableFile, profileState.configuration().getExeArgs().args);
    }
}
