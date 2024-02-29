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

import com.falsepattern.zigbrains.project.openapi.components.ZigProjectSettingsService;
import com.falsepattern.zigbrains.zig.environment.ZLSConfig;
import com.falsepattern.zigbrains.zig.environment.ZLSConfigProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class ToolchainZLSConfigProvider implements ZLSConfigProvider {
    @Override
    public @NotNull ZLSConfig getEnvironment(Project project) {
        val projectSettings = ZigProjectSettingsService.getInstance(project);
        val toolchain = projectSettings.getToolchain();
        if (toolchain == null)
            return ZLSConfig.EMPTY;
        val projectDir = ProjectUtil.guessProjectDir(project);
        val env = toolchain.zig().getEnv(projectDir == null ? Path.of(".") : projectDir.toNioPath());
        return env.map(e -> new ZLSConfig(e.zigExecutable(), e.libDirectory())).orElse(ZLSConfig.EMPTY);
    }
}
