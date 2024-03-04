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

package com.falsepattern.zigbrains.project.console;

import com.falsepattern.zigbrains.common.util.FileUtil;
import com.falsepattern.zigbrains.project.openapi.module.ZigModuleType;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class ZigSourceFileFilter implements Filter {
    private final Project project;
    @Nullable
    @Override
    public Result applyFilter(@NotNull String line, int entireLength) {
        val lineStart = entireLength - line.length();
        int splitA, splitB, splitC;
        splitA = line.indexOf(':');
        if (splitA < 0)
            return null;
        splitB = line.indexOf(':', splitA + 1);
        if (splitB < 0)
            return null;
        splitC = line.indexOf(':', splitB + 1);
        if (splitC < 0)
            return null;
        final int lineNumber, lineOffset;
        try {
            lineNumber = Math.max(Integer.parseInt(line, splitA + 1, splitB, 10) - 1, 0);
            lineOffset = Math.max(Integer.parseInt(line, splitB + 1, splitC, 10) - 1, 0);
        } catch (NumberFormatException ignored) {
            return null;
        }
        val pathStr = line.substring(0, splitA);
        var path = Path.of(pathStr);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            val projectPath = project.getBasePath();
            if (projectPath == null)
                return null;
            path = Path.of(projectPath, pathStr);
            if (!Files.exists(path) || !Files.isRegularFile(path))
                return null;
        }
        val file = FileUtil.virtualFileFromURI(path.toUri());
        return new Result(lineStart, lineStart + splitC, new OpenFileHyperlinkInfo(project, file, lineNumber, lineOffset));
    }
}
