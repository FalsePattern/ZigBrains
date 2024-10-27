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

import com.falsepattern.zigbrains.common.direnv.DirenvCmd;
import com.intellij.openapi.util.UserDataHolder;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.falsepattern.zigbrains.project.toolchain.AbstractZigToolchain.WORK_DIR_KEY;

public class ZigSystemPathToolchainFlavour extends AbstractZigToolchainFlavour{
    @Override
    protected CompletableFuture<List<Path>> getHomePathCandidates(@NotNull UserDataHolder data) {
        val workDir = data.getUserData(WORK_DIR_KEY);
        val direnv = data.getUserData(DirenvCmd.DIRENV_KEY);
        CompletableFuture<Map<String, String>> direnvFuture;
        if (direnv == Boolean.TRUE && DirenvCmd.direnvInstalled() && workDir != null) {
            val cmd = new DirenvCmd(workDir);
            direnvFuture = cmd.importDirenvAsync();
        } else {
            direnvFuture = CompletableFuture.completedFuture(Map.of());
        }
        return direnvFuture.thenApplyAsync(env -> {
            val PATH = env.getOrDefault("PATH", System.getenv("PATH"));
            if (PATH == null) {
                return Collections.emptyList();
            }
            return Arrays.stream(PATH.split(File.pathSeparator))
                         .filter(it -> !it.isEmpty())
                         .map(Path::of)
                         .filter(Files::isDirectory)
                         .toList();
        });
    }
}
