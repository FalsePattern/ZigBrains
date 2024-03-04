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

package com.falsepattern.zigbrains.project.execution.binary;

import com.falsepattern.zigbrains.project.execution.base.ProfileStateBase;
import com.falsepattern.zigbrains.project.toolchain.AbstractZigToolchain;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.runners.ExecutionEnvironment;
import lombok.val;

import java.nio.charset.StandardCharsets;

public class ProfileStateBinary extends ProfileStateBase<ZigExecConfigBinary> {
    public ProfileStateBinary(ExecutionEnvironment environment, ZigExecConfigBinary configuration) {
        super(environment, configuration);
    }

    @Override
    public GeneralCommandLine getCommandLine(AbstractZigToolchain toolchain, boolean debug) throws ExecutionException {
        val cli = new GeneralCommandLine();
        val cfg = configuration();
        cfg.getWorkingDirectory().getPath().ifPresent(dir -> cli.setWorkDirectory(dir.toFile()));
        cli.setExePath(cfg.getExePath().getPath().orElseThrow(() -> new ExecutionException("Missing executable path")).toString());
        cli.setCharset(StandardCharsets.UTF_8);
        cli.setRedirectErrorStream(true);
        cli.addParameters(cfg.getArgs().args);
        return cli;
    }
}
