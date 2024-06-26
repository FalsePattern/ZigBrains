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

package com.falsepattern.zigbrains.common.util;

import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public class PathUtil {
    public static boolean hasExecutable(Path path, String toolName) {
        return Files.isExecutable(pathToExecutable(path, toolName));
    }

    public static Path pathToExecutable(Path path, String toolName) {
        var exeName = SystemInfo.isWindows ? toolName + ".exe" : toolName;
        return path.resolve(exeName).toAbsolutePath();
    }

    public static @Nullable Path pathFromString(@Nullable String pathString) {
        if (pathString == null || pathString.isBlank()) {
            return null;
        }
        try {
            return Path.of(pathString);
        } catch (InvalidPathException e) {
            return null;
        }
    }

    public static @NotNull String stringFromPath(@Nullable Path path) {
        if (path == null) {
            return "";
        }
        return path.toString();
    }
}
