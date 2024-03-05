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

package com.falsepattern.zigbrains.zig.debugger.runner.base;

import com.falsepattern.zigbrains.project.execution.base.ProfileStateBase;
import com.falsepattern.zigbrains.project.toolchain.AbstractZigToolchain;
import com.falsepattern.zigbrains.project.util.CLIUtil;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.jetbrains.cidr.execution.Installer;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@RequiredArgsConstructor
public abstract class ZigDebugEmitBinaryInstaller<ProfileState extends ProfileStateBase<?>> implements Installer {
    protected final String kind;
    protected final ProfileState profileState;
    protected final AbstractZigToolchain toolchain;
    private File executableFile;

    @Override
    public @NotNull GeneralCommandLine install() throws ExecutionException {
        val commandLine = profileState.getCommandLine(toolchain, true);
        final Path tmpDir;
        try {
            tmpDir = Files.createTempDirectory("zigbrains_debug").toAbsolutePath();
        } catch (IOException e) {
            throw new ExecutionException("Failed to create temporary directory for " + kind + " binary", e);
        }
        val exe = tmpDir.resolve("executable").toFile();
        commandLine.addParameters("-femit-bin=" + exe.getAbsolutePath());
        val outputOpt = CLIUtil.execute(commandLine, Integer.MAX_VALUE);
        if (outputOpt.isEmpty()) {
            throw new ExecutionException("Failed to execute \"zig " + commandLine.getParametersList().getParametersString() + "\"!");
        }
        val output = outputOpt.get();
        if (output.getExitCode() != 0) {
            throw new ExecutionException("Zig compilation failed with exit code " + output.getExitCode() + "\nError output:\n" + output.getStdout() + "\n" + output.getStderr());
        }
        //Find our binary
        try (val stream = Files.list(tmpDir)){
            executableFile = stream.filter(file -> !file.getFileName().toString().endsWith(".o"))
                                   .map(Path::toFile)
                                   .filter(File::canExecute)
                                   .findFirst()
                                   .orElseThrow(() -> new IOException("No executable file present in temporary directory \"" +
                                                                      tmpDir + "\""));
        } catch (Exception e) {
            throw new ExecutionException("Failed to find compiled binary", e);
        }

        //Construct new command line
        val cfg = profileState.configuration();
        val cli = new GeneralCommandLine().withExePath(executableFile.getAbsolutePath());
        cfg.getWorkingDirectory().getPath().ifPresent(x -> cli.setWorkDirectory(x.toFile()));
        cli.addParameters(getExeArgs());
        cli.withCharset(StandardCharsets.UTF_8);
        cli.withRedirectErrorStream(true);
        return cli;
    }

    public abstract String[] getExeArgs();

    @Override
    public @NotNull File getExecutableFile() {
        return executableFile;
    }
}
