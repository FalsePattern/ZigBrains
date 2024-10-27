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

import com.falsepattern.zigbrains.common.util.Lazy;
import com.falsepattern.zigbrains.project.toolchain.flavours.AbstractZigToolchainFlavour;
import com.falsepattern.zigbrains.project.toolchain.tools.ZigCompilerTool;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.util.concurrency.AppExecutorUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@Getter
public abstract class AbstractZigToolchain {
    private final Path location;
    private final @Nullable Project project;

    private final Lazy<ZigCompilerTool> zig = new Lazy<>(() -> new ZigCompilerTool(this));
    public static final Key<Path> WORK_DIR_KEY = Key.create("ZIG_TOOLCHAIN_WORK_DIR");
    public static final Key<Project> PROJECT_KEY = Key.create("ZIG_TOOLCHAIN_PROJECT");

    public static @NotNull CompletableFuture<@Nullable AbstractZigToolchain> suggest(@NotNull UserDataHolder workDir) {
        val project = workDir.getUserData(PROJECT_KEY);
        if (workDir.getUserData(WORK_DIR_KEY) == null) {
            if (project != null) {
                val projectDir = ProjectUtil.guessProjectDir(project);
                if (projectDir != null) {
                    workDir.putUserData(WORK_DIR_KEY, projectDir.toNioPath());
                }
            }
        }
        val exec = AppExecutorUtil.getAppExecutorService();
        val homePathFutures = AbstractZigToolchainFlavour.getApplicableFlavours()
                                                         .stream()
                                                         .map(it -> it.suggestHomePaths(workDir))
                                                         .toList();
        return CompletableFuture.allOf(homePathFutures.toArray(CompletableFuture[]::new))
                                .thenApplyAsync(it -> homePathFutures.stream()
                                                                     .map(CompletableFuture::join)
                                                                     .flatMap(Collection::stream)
                                                                     .filter(Objects::nonNull)
                                                                     .map((dir) -> ZigToolchainProvider.findToolchain(dir, project))
                                                                     .filter(Objects::nonNull)
                                                                     .findFirst()
                                                                     .orElse(null), exec);
    }

    public ZigCompilerTool zig() {
        return zig.get();
    }

    public abstract int executionTimeoutInMilliseconds();

    public abstract @NotNull GeneralCommandLine patchCommandLine(@NotNull GeneralCommandLine commandLine, @NotNull UserDataHolder data);

    public abstract Path pathToExecutable(String toolName);

    public @NotNull UserDataHolder getDataForSelfRuns() {
        return new UserDataHolderBase();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AbstractZigToolchain azt) {
            return Objects.equals(location, azt.location);
        }
        return false;
    }
}
