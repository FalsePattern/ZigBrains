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

package com.falsepattern.zigbrains.zig.debugger.win;

import com.falsepattern.zigbrains.zig.debugger.dap.DAPDebuggerDriverConfiguration;
import com.falsepattern.zigbrains.zig.debugger.win.config.WinDebuggerConfigService;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.jetbrains.cidr.ArchitectureType;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver;
import lombok.val;
import org.apache.commons.io.file.PathUtils;
import org.eclipse.lsp4j.debug.InitializeRequestArguments;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.ref.Cleaner;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public class WinDebuggerDriverConfiguration extends DAPDebuggerDriverConfiguration {
    private static void extractVSDebugger(Path pathToPluginFile, Path pathToExtracted) throws IOException {
        if (pathToPluginFile == null) {
            throw new IllegalArgumentException("Please set the debugger inside Build, Execution, Deployment | Debugger | Zig (Windows)");
        }
        if (!Files.isRegularFile(pathToPluginFile) || !pathToPluginFile.getFileName().toString().endsWith(".vsix")) {
            throw new IllegalArgumentException("Invalid debugger file path! Please check Build, Execution, Deployment | Debugger | Zig (Windows) again! The file MUST be a .vsix file!");
        }
        URI uri;
        try {
            uri = new URI("jar:" + pathToPluginFile.toAbsolutePath().toUri().toASCIIString());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Could not parse plugin file path: " + pathToPluginFile);
        }
        try (val fs = FileSystems.newFileSystem(uri, Map.of())) {
            val basePath = fs.getPath("/extension/debugAdapters/vsdbg/bin");
            try (val walk = Files.walk(basePath)) {
                walk.forEach(path -> {
                    if (!Files.isRegularFile(path))
                        return;
                    val relPath = Path.of(basePath.relativize(path).toString());
                    val resPath = pathToExtracted.resolve(relPath);
                    try {
                        Files.createDirectories(resPath.getParent());
                        Files.copy(path, resPath, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    @Override
    public @NotNull GeneralCommandLine createDriverCommandLine(@NotNull DebuggerDriver debuggerDriver, @NotNull ArchitectureType architectureType) {
        val pathToPluginFileStr = WinDebuggerConfigService.getInstance().cppToolsPath;
        if (pathToPluginFileStr == null || pathToPluginFileStr.isBlank()) {
            throw new IllegalArgumentException("Please set the debugger inside Build, Execution, Deployment | Debugger | Zig (Windows)");
        }
        Path pathToPluginFile;
        try {
            pathToPluginFile = Path.of(pathToPluginFileStr);
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("Invalid debugger path " + pathToPluginFileStr + "! Make sure it points to the .vsix file!");
        }
        Path pathToExtracted;
        try {
            val tmpDir = Path.of(System.getProperty("java.io.tmpdir"));
            int i = 0;
            do {
                pathToExtracted = tmpDir.resolve("zb-windbg-" + i++);
            } while (Files.exists(pathToExtracted.resolve(".lock")) || Files.isRegularFile(pathToExtracted));
            if (Files.exists(pathToExtracted)) {
                PathUtils.deleteDirectory(pathToExtracted);
            }
            extractVSDebugger(pathToPluginFile, pathToExtracted);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Path finalPathToExtracted = pathToExtracted;
        val lockFile = finalPathToExtracted.resolve(".lock");
        try {
            Files.createFile(lockFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Cleaner.create().register(debuggerDriver, () -> {
            try {
                Files.delete(lockFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        val cli = new GeneralCommandLine();
        cli.setExePath(finalPathToExtracted.resolve("vsdbg.exe").toString());
        cli.setCharset(StandardCharsets.UTF_8);
        cli.addParameters("--interpreter=vscode", "--extConfigDir=%USERPROFILE%\\.cppvsdbg\\extensions");
        cli.setWorkDirectory(finalPathToExtracted.toString());
        return cli;
    }

    @Override
    public @NotNull String getDriverName() {
        return "WinDAPDriver";
    }

    @Override
    public @NotNull DebuggerDriver createDriver(DebuggerDriver.@NotNull Handler handler, @NotNull ArchitectureType architectureType)
            throws ExecutionException {
        return new WinDAPDriver(handler, this);
    }

    @Override
    public void customizeInitializeArguments(InitializeRequestArguments initArgs) {
        initArgs.setPathFormat("path");
        initArgs.setAdapterID("cppvsdbg");
    }
}
