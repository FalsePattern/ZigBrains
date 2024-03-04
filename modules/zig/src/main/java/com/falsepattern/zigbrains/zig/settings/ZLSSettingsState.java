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

package com.falsepattern.zigbrains.zig.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Service(Service.Level.PROJECT)
@State(name = "ZLSSettings",
       storages = @Storage("zigbrains.xml"))
public final class ZLSSettingsState implements PersistentStateComponent<ZLSSettingsState> {
    public String zlsPath = "";
    public String zlsConfigPath = "";
    public boolean initialAutodetectHasBeenDone = false;
    public boolean increaseTimeouts = false;
    public boolean asyncFolding = true;
    public boolean debug = false;
    public boolean messageTrace = false;

    public boolean buildOnSave = false;
    public String buildOnSaveStep = "install";
    public boolean dangerousComptimeExperimentsDoNotEnable = false;
    public boolean highlightGlobalVarDeclarations = false;

    public static Optional<String> executablePathFinder(String exe) {
        var exeName = SystemInfo.isWindows ? exe + ".exe" : exe;
        var PATH = System.getenv("PATH").split(File.pathSeparator);
        for (var dir: PATH) {
            var path = Path.of(dir);
            try {
                path = path.toAbsolutePath();
            } catch (Exception ignored) {
                continue;
            }
            if (!Files.exists(path) || !Files.isDirectory(path)) {
                continue;
            }
            var exePath = path.resolve(exeName).toAbsolutePath();
            if (!Files.isRegularFile(exePath) || !Files.isExecutable(exePath)) {
                continue;
            }
            return Optional.of(exePath.toString());
        }
        return Optional.empty();
    }

    public static ZLSSettingsState getInstance(Project project) {
        return project.getService(ZLSSettingsState.class);
    }

    @Override
    public ZLSSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull ZLSSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
