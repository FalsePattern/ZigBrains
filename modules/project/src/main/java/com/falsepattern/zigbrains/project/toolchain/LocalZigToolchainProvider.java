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

package com.falsepattern.zigbrains.project.toolchain;

import com.intellij.execution.wsl.WslPath;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Map;

public class LocalZigToolchainProvider implements ZigToolchainProvider {
    private static final Map<Path, LocalZigToolchain> tcCache = ContainerUtil.createWeakKeyWeakValueMap();
    @Override
    public @Nullable AbstractZigToolchain getToolchain(Path homePath, @Nullable Project project) {
        if (SystemInfo.isWindows && WslPath.isWslUncPath(homePath.toString())) {
            return null;
        }

        return tcCache.computeIfAbsent(homePath, (path) -> new LocalZigToolchain(path, project));
    }
}
