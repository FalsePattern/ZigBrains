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

package com.falsepattern.zigbrains.debugger.runner.base;

import com.falsepattern.zigbrains.debugger.Utils;
import com.falsepattern.zigbrains.project.execution.base.ProfileStateBase;
import com.falsepattern.zigbrains.project.toolchain.AbstractZigToolchain;
import com.intellij.execution.ExecutionException;
import com.intellij.openapi.util.io.FileUtil;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration;
import lombok.val;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class ZigDebugParametersEmitBinaryBase<ProfileState extends ProfileStateBase<?>> extends ZigDebugParametersBase<ProfileState> implements PreLaunchAware {
    protected volatile File executableFile;
    private final String kind;
    public ZigDebugParametersEmitBinaryBase(DebuggerDriverConfiguration driverConfiguration, AbstractZigToolchain toolchain, ProfileState profileState, String kind) {
        super(driverConfiguration, toolchain, profileState);
        this.kind = kind;
    }

    private File compileExe()
            throws ExecutionException, Utils.ProcessException {
        final File executableFile;
        val commandLine = profileState.getCommandLine(toolchain, true);
        final Path tmpDir;
        try {
            tmpDir = FileUtil.createTempDirectory("zigbrains_debug", "", true).toPath();
        } catch (IOException e) {
            throw new ExecutionException("Failed to create temporary directory for " + kind + " binary", e);
        }
        val exe = tmpDir.resolve("executable").toFile();
        commandLine.addParameters("-femit-bin=" + exe.getAbsolutePath());
        Utils.executeCommandLineWithErrorChecks(commandLine);
        //Find our binary
        try (val stream = Files.list(tmpDir)){
            executableFile = stream.filter(file -> !file.getFileName().toString().endsWith(".o"))
                                   .map(Path::toFile)
                                   .filter(File::canExecute)
                                   .findFirst()
                                   .orElseThrow(() -> new IOException("No executable file present in temporary directory \"" +
                                                                      tmpDir + "\""));
        } catch (Exception e) {
            throw new ExecutionException("Failed to find compiled binary! " + e.getMessage(), e);
        }
        return executableFile;
    }

    @Override
    public void preLaunch() throws Exception {
        this.executableFile = compileExe();
    }

}
