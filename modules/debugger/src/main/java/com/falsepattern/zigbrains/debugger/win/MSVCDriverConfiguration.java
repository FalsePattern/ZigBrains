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

package com.falsepattern.zigbrains.debugger.win;

import com.falsepattern.zigbrains.debugger.dap.DAPDebuggerDriverConfiguration;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.jetbrains.cidr.ArchitectureType;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver;
import lombok.val;
import org.eclipse.lsp4j.debug.InitializeRequestArguments;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public abstract class MSVCDriverConfiguration extends DAPDebuggerDriverConfiguration {
    protected abstract Path getDebuggerExecutable();

    @Override
    public @NotNull DebuggerDriver createDriver(DebuggerDriver.@NotNull Handler handler, @NotNull ArchitectureType architectureType)
            throws ExecutionException {
        return new WinDAPDriver(handler, this);
    }

    @Override
    public @NotNull GeneralCommandLine createDriverCommandLine(
            @NotNull DebuggerDriver debuggerDriver, @NotNull ArchitectureType architectureType)
            throws ExecutionException {
        val path = getDebuggerExecutable();
        val cli = new GeneralCommandLine();
        cli.setExePath(path.toString());
        cli.addParameters("--interpreter=vscode", "--extConfigDir=%USERPROFILE%\\.cppvsdbg\\extensions");
        cli.setWorkDirectory(path.getParent().toString());
        return cli;
    }

    @Override
    public void customizeInitializeArguments(InitializeRequestArguments initArgs) {
        initArgs.setPathFormat("path");
        initArgs.setAdapterID("cppvsdbg");
    }
}
