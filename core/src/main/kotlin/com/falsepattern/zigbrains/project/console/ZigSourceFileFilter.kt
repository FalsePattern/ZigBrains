/*
 * This file is part of ZigBrains.
 *
 * Copyright (C) 2023-2024 FalsePattern
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
import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.vfs.refreshAndFindVirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import java.nio.file.InvalidPathException
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.math.max


class ZigSourceFileFilter(private val project: Project): Filter {
    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        val lineStart = entireLength - line.length
        val projectPath = project.guessProjectDir()?.toNioPathOrNull()
        val results = ArrayList<ResultItem>()
        val matcher = LEN_REGEX.findAll(line)
        for (match in matcher) {
            val start = match.range.first
            val pair = findLongestParsablePathFromOffset(line, start, projectPath)
            val path = pair?.first ?: return null
            val file = path.refreshAndFindVirtualFile() ?: return null
            val lineNumber = max(match.groups[1]!!.value.toInt() - 1, 0)
            val lineOffset = max(match.groups[2]!!.value.toInt() - 1, 0)
            results.add(ResultItem(lineStart + pair.second, lineStart + match.range.last + 1, OpenFileHyperlinkInfo(project, file, lineNumber, lineOffset)))
        }
        return Filter.Result(results)
    }

    private fun findLongestParsablePathFromOffset(line: String, end: Int, projectPath: Path?): Pair<Path, Int>? {
        var longestStart = -1
        var longest: Path? = null
        for (i in end - 1 downTo 0) {
            try {
                val pathStr = line.substring(i, end)
                var path: Path = pathStr.toNioPathOrNull() ?: continue
                var file = path.toFile()
                if ((!file.exists() || !path.isRegularFile()) && projectPath != null) {
                    path = projectPath.resolve(pathStr)
                    file = path.toFile()
                    if (!file.exists() || !path.isRegularFile())
                        continue
                }
                longest = path
                longestStart = i
            } catch (ignored: InvalidPathException) {
            }
        }
        longest ?: return null
        return Pair(longest, longestStart)
    }
}

private val LEN_REGEX = Regex(":(\\d+):(\\d+)")