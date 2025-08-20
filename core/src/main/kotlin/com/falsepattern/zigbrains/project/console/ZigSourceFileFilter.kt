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
        if (line.isEmpty())
            return null
        val lineStart = entireLength - line.length
        val results = ArrayList<ResultItem>()
        var prevEnd = 0
        for (match in findLengthMarkers(line)) {
            val start = match.start
            val pair = findLongestParsablePathFromOffset(line, prevEnd, start) ?: return null
            prevEnd = match.end
            val path = pair.first
            val lineNumber = max(match.line - 1, 0)
            val lineOffset = max(match.offset - 1, 0)
            results.add(ResultItem(lineStart + pair.second, lineStart + match.end, LazyOpenFileHyperlinkInfo(project, path, lineNumber, lineOffset)))
        }
        if (results.isEmpty())
            return null
        return Filter.Result(results)
    }

    private fun findLongestParsablePathFromOffset(line: String, start: Int, end: Int): Pair<Path, Int>? {
        for (i in 0 ..< end) {
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
                return Pair(file.toPath(), i)
            } catch (_: InvalidPathException) {
            }
        }
        return null
    }
}

private data class LengthMarker(val start: Int, val end: Int, val line: Int, val offset: Int)

private fun findLengthMarkers(line: String): Sequence<LengthMarker> = sequence {
    val chars = line.toCharArray()
    val len = chars.size
    var i = 0
    while (true) {
        while (chars[i] != ':') {
            i++
            if (i == len)
                return@sequence
        }
        val start = i
        i++
        if (i == len)
            return@sequence
        var line = 0
        while (chars[i] in '0'..'9') {
            line *= 10
            line += chars[i] - '0'
            i++
            if (i == len)
                return@sequence
        }
        if (chars[i] != ':')
            continue
        i++
        if (i == len)
            return@sequence
        var offset = 0
        while (chars[i] in '0'..'9') {
            offset *= 10
            offset += chars[i] - '0'
            i++
            if (i == len)
                return@sequence
        }
        yield(LengthMarker(start, i, line, offset))
    }
}
