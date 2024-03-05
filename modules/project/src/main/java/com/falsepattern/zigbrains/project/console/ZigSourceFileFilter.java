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
import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.openapi.project.Project;
import kotlin.Pair;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class ZigSourceFileFilter implements Filter {
    private final Project project;
    private final Pattern LEN_REGEX = Pattern.compile(":(\\d+):(\\d+)");

    private Pair<Path, Integer> findLongestParsablePathFromOffset(String line, int end, String projectPath) {
        int longestStart = -1;
        Path longest = null;
        for (int i = end - 1; i >= 0; i--) {
            try {
                val pathStr = line.substring(i, end);
                var path = Path.of(pathStr);
                if (!Files.exists(path) || !Files.isRegularFile(path)) {
                    path = Path.of(projectPath, pathStr);
                    if (!Files.exists(path) || !Files.isRegularFile(path))
                        continue;
                }
                longest = path;
                longestStart = i;
            } catch (InvalidPathException ignored){}
        }
        return new Pair<>(longest, longestStart);
    }

    @Nullable
    @Override
    public Result applyFilter(@NotNull String line, int entireLength) {
        val lineStart = entireLength - line.length();
        val projectPath = project.getBasePath();
        val results = new ArrayList<ResultItem>();
        val matcher = LEN_REGEX.matcher(line);
        while (matcher.find()) {
            val end = matcher.start();
            val pair = findLongestParsablePathFromOffset(line, end, projectPath);
            val path = pair.getFirst();
            if (path == null)
                return null;
            val lineNumber = Math.max(Integer.parseInt(matcher.group(1)) - 1, 0);
            val lineOffset = Math.max(Integer.parseInt(matcher.group(2)) - 1, 0);
            val file = FileUtil.virtualFileFromURI(path.toUri());
            results.add(new ResultItem(lineStart + pair.getSecond(), lineStart + matcher.end(), new OpenFileHyperlinkInfo(project, file, lineNumber, lineOffset)));

        }
        return new Result(results);
    }
}
