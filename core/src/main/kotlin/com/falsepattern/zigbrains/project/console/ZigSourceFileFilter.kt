/*
 * This file is part of ZigBrains.
 *
 * Copyright (C) 2023-2025 FalsePattern
 * All Rights Reserved
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * ZigBrains is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 *
 * ZigBrains is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ZigBrains. If not, see <https://www.gnu.org/licenses/>.
 */

package com.falsepattern.zigbrains.project.console

import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.Filter.ResultItem
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.toNioPathOrNull
import java.io.File
import java.nio.file.InvalidPathException
import java.nio.file.Path
import kotlin.math.max


class ZigSourceFileFilter(private val project: Project): Filter {
    private val projectPath = runCatching { project.guessProjectDir()?.toNioPathOrNull()?.toFile() }.getOrNull()
    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        val lineStart = entireLength - line.length
        val results = ArrayList<ResultItem>()
        val matcher = LEN_REGEX.findAll(line)
        for (match in matcher) {
            val start = match.range.first
            val pair = findLongestParsablePathFromOffset(line, start) ?: return null
            val path = pair.first
            val lineNumber = max(match.groups[1]!!.value.toInt() - 1, 0)
            val lineOffset = max(match.groups[2]!!.value.toInt() - 1, 0)
            results.add(ResultItem(lineStart + pair.second, lineStart + match.range.last + 1, LazyOpenFileHyperlinkInfo(project, path, lineNumber, lineOffset)))
        }
        return Filter.Result(results)
    }

    private fun findLongestParsablePathFromOffset(line: String, end: Int): Pair<Path, Int>? {
        var longestStart = -1
        var longest: File? = null
        for (i in end - 1 downTo 0) {
            try {
                val pathStr = line.substring(i, end)
                var file = File(pathStr)
                if (!file.isFile) {
                    if (projectPath == null) {
                        continue
                    }
                    file = projectPath.resolve(pathStr)
                    if (!file.isFile) {
                        continue
                    }
                }
                longest = file
                longestStart = i
            } catch (ignored: InvalidPathException) {
            }
        }
        longest ?: return null
        return Pair(longest.toPath(), longestStart)
    }
}

private val LEN_REGEX = Regex(":(\\d+):(\\d+)")