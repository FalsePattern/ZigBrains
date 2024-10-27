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

package com.falsepattern.zigbrains.project.toolchain.flavours;

import com.falsepattern.zigbrains.common.util.PathUtil;
import com.falsepattern.zigbrains.project.toolchain.tools.ZigCompilerTool;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractZigToolchainFlavour {
    private static final ExtensionPointName<AbstractZigToolchainFlavour> EXTENSION_POINT_NAME =
            ExtensionPointName.create("com.falsepattern.zigbrains.toolchainFlavour");

    public static @NotNull List<AbstractZigToolchainFlavour> getApplicableFlavours() {
        return EXTENSION_POINT_NAME.getExtensionList()
                                   .stream()
                                   .filter(AbstractZigToolchainFlavour::isApplicable)
                                   .toList();
    }

    public static @Nullable AbstractZigToolchainFlavour getFlavour(Path path) {
        return getApplicableFlavours().stream()
                                      .filter(flavour -> flavour.isValidToolchainPath(path))
                                      .findFirst()
                                      .orElse(null);
    }

    public CompletableFuture<List<Path>> suggestHomePaths(@NotNull UserDataHolder data) {
        return getHomePathCandidates(data).thenApplyAsync(it -> it.stream()
                                                                  .filter(this::isValidToolchainPath)
                                                                  .toList(),
                                                             AppExecutorUtil.getAppExecutorService());
    }

    protected abstract CompletableFuture<List<Path>> getHomePathCandidates(@NotNull UserDataHolder data);

    protected boolean isApplicable() {
        return true;
    }

    protected boolean isValidToolchainPath(Path path) {
        return Files.isDirectory(path) && PathUtil.hasExecutable(path, ZigCompilerTool.TOOL_NAME);
    }
}
