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

import com.falsepattern.zigbrains.project.execution.base.ProfileStateBase;
import com.falsepattern.zigbrains.project.toolchain.AbstractZigToolchain;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.jetbrains.cidr.execution.Installer;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
public class ZigDebugEmitBinaryInstaller<ProfileState extends ProfileStateBase<?>> implements Installer {
    protected final ProfileState profileState;
    protected final AbstractZigToolchain toolchain;
    private final File executableFile;
    private final String[] exeArgs;

    @Override
    public @NotNull GeneralCommandLine install() {
        //Construct new command line
        val cfg = profileState.configuration();
        val cli = new GeneralCommandLine().withExePath(executableFile.getAbsolutePath());
        cfg.getWorkingDirectory().getPath().ifPresent(x -> cli.setWorkDirectory(x.toFile()));
        cli.addParameters(exeArgs);
        cli.withCharset(StandardCharsets.UTF_8);
        cli.withRedirectErrorStream(true);
        return profileState.configuration().patchCommandLine(cli, toolchain);
    }

    @Override
    public @NotNull File getExecutableFile() {
        return executableFile;
    }
}
