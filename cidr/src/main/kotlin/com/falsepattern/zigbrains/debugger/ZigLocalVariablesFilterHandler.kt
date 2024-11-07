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

package com.falsepattern.zigbrains.debugger

import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.falsepattern.zigbrains.zig.ZigFileType
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XSourcePosition
import com.jetbrains.cidr.execution.debugger.backend.LLValue
import com.jetbrains.cidr.execution.debugger.evaluation.LocalVariablesFilterHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import java.util.concurrent.CompletableFuture

class ZigLocalVariablesFilterHandler: LocalVariablesFilterHandler {
    override fun filterVars(proj: Project, pos: XSourcePosition, vars: List<LLValue>): CompletableFuture<List<LLValue>> {
        return proj.zigCoroutineScope.async {
            val vf = pos.file
            if (vf.fileType == ZigFileType) {
                return@async ArrayList(vars)
            }
            return@async listOf()
        }.asCompletableFuture()
    }

    override fun canFilterAtPos(proj: Project, pos: XSourcePosition): Boolean {
        return pos.file.fileType == ZigFileType
    }
}