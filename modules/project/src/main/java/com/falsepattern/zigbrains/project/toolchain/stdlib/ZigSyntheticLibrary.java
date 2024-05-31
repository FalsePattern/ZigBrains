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

package com.falsepattern.zigbrains.project.toolchain.stdlib;

import com.falsepattern.zigbrains.common.util.PathUtil;
import com.falsepattern.zigbrains.project.openapi.components.ZigProjectSettingsService;
import com.falsepattern.zigbrains.zig.Icons;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.SyntheticLibrary;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@RequiredArgsConstructor
public class ZigSyntheticLibrary extends SyntheticLibrary implements ItemPresentation {
    private final Future<Collection<VirtualFile>> roots;
    private final Future<String> name;
    public ZigSyntheticLibrary(Project project) {
        val service = ZigProjectSettingsService.getInstance(project);
        val state = service.getState();
        this.roots = ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var roots = pathToVFS(state.getExplicitPathToStd());
            if (roots != null) {
                return roots;
            }
            val toolchain = state.getToolchain();
            if (toolchain != null) {
                val stdPath =
                        toolchain.zig().getStdPath().orElse(null);
                return pathToVFS(stdPath);
            }
            return Collections.emptySet();
        });

        this.name = ApplicationManager.getApplication()
                                      .executeOnPooledThread(() -> Optional.ofNullable(state.getToolchain())
                                                                           .flatMap(tc -> tc.zig().queryVersion())
                                                                           .map(version -> "Zig " + version)
                                                                           .orElse("Zig"));
    }

    private static @Nullable Collection<VirtualFile> pathToVFS(String path) {
        if (path != null && !path.isEmpty()) {
            val thePath = PathUtil.pathFromString(path);
            if (thePath != null) {
                val file = VfsUtil.findFile(thePath, true);
                if (file != null) {
                    val children = file.getChildren();
                    if (children != null && children.length > 0)
                        return Arrays.asList(children);
                }
            }
        }
        return null;
    }
    @Override
    public @NotNull Collection<VirtualFile> getSourceRoots() {
        try {
            return roots.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return Collections.emptySet();
        }
    }

    @Override
    public @NotNull Collection<VirtualFile> getBinaryRoots() {
        return super.getBinaryRoots();
    }

    @SneakyThrows
    @Override
    public boolean equals(Object o) {
        return o instanceof ZigSyntheticLibrary other && Objects.equals(roots.get(), other.roots.get());
    }

    @SneakyThrows
    @Override
    public int hashCode() {
        return Objects.hash(roots.get());
    }

    @Override
    public @NlsSafe @Nullable String getPresentableText() {
        try {
            return name.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return "Zig";
        }
    }

    @Override
    public @Nullable Icon getIcon(boolean unused) {
        return Icons.ZIG;
    }
}
