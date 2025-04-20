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

package com.falsepattern.zigbrains.project.run

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.execution.process.KillableProcessHandler
import com.pty4j.PtyProcess
import java.nio.charset.Charset

open class ZigProcessHandler : KillableProcessHandler {
    constructor(commandLine: GeneralCommandLine) : super(commandLine) {
        setHasPty(commandLine is PtyCommandLine)
        setShouldDestroyProcessRecursively(!hasPty())
    }

    protected constructor (process: Process, commandLine: String, charset: Charset) : super(process, commandLine, charset) {
        setHasPty(process is PtyProcess)
        setShouldDestroyProcessRecursively(!hasPty())
    }

    class IPCAware : ZigProcessHandler {
        val originalCommandLine: String
        constructor(originalCommandLine: String, commandLine: GeneralCommandLine) : super(commandLine) {
            this.originalCommandLine = originalCommandLine
        }

        fun unwrap(): ZigProcessHandler {
            return ZigProcessHandler(this.process, this.originalCommandLine, this.charset ?: Charsets.UTF_8)
        }
    }
}