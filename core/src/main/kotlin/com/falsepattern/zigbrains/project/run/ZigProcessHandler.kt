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

package com.falsepattern.zigbrains.project.run

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.execution.process.AnsiEscapeDecoder.ColoredTextAcceptor
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.openapi.util.Key
import com.pty4j.PtyProcess
import java.nio.charset.Charset

class ZigProcessHandler : KillableColoredProcessHandler, ColoredTextAcceptor {
    constructor(commandLine: GeneralCommandLine) : super(commandLine) {
        setHasPty(commandLine is PtyCommandLine)
        setShouldDestroyProcessRecursively(!hasPty())
    }

    constructor (process: Process, commandLine: String, charset: Charset) : super(process, commandLine, charset) {
        setHasPty(process is PtyProcess)
        setShouldDestroyProcessRecursively(!hasPty())
    }

    override fun coloredTextAvailable(text: String, attributes: Key<*>) {
        super.coloredTextAvailable(text.translateVT100Escapes(), attributes)
    }
}

private val VT100_CHARS = CharArray(256).apply {
    this.fill(' ')
    this[0x6A] = '┘';
    this[0x6B] = '┐';
    this[0x6C] = '┌';
    this[0x6D] = '└';
    this[0x6E] = '┼';
    this[0x71] = '─';
    this[0x74] = '├';
    this[0x75] = '┤';
    this[0x76] = '┴';
    this[0x77] = '┬';
    this[0x78] = '│';
}

private const val VT100_BEGIN_SEQ = "\u001B(0"
private const val VT100_END_SEQ = "\u001B(B"
private const val VT100_BEGIN_SEQ_LENGTH: Int = VT100_BEGIN_SEQ.length
private const val VT100_END_SEQ_LENGTH: Int = VT100_END_SEQ.length

private fun String.translateVT100Escapes(): String {
    var offset = 0
    val result = StringBuilder()
    val textLength = length
    while (offset < textLength) {
        val startIndex = indexOf(VT100_BEGIN_SEQ, offset)
        if (startIndex < 0) {
            result.append(substring(offset, textLength).replace(VT100_END_SEQ, ""))
            break
        }
        result.append(this, offset, startIndex)
        val blockOffset = startIndex + VT100_BEGIN_SEQ_LENGTH
        var endIndex = indexOf(VT100_END_SEQ, blockOffset)
        if (endIndex < 0) {
            endIndex = textLength
        }
        for (i in blockOffset until endIndex) {
            val c = this[i].code
            if (c >= 256) {
                result.append(c)
            } else {
                result.append(VT100_CHARS[c])
            }
        }
        offset = endIndex + VT100_END_SEQ_LENGTH
    }
    return result.toString()
}
