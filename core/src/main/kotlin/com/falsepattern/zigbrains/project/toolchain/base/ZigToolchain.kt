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

package com.falsepattern.zigbrains.project.toolchain.base

import com.falsepattern.zigbrains.project.toolchain.tools.ZigCompilerTool
import com.falsepattern.zigbrains.shared.NamedObject
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.MapAnnotation
import java.nio.file.Path

/**
 * These MUST be stateless and interchangeable! (e.g., immutable data class)
 */
interface ZigToolchain: NamedObject<ZigToolchain> {
    val zig: ZigCompilerTool get() = ZigCompilerTool(this)

    fun <T> getUserData(key: Key<T>): T?

    /**
     * Returned type must be the same class
     */
    fun <T> withUserData(key: Key<T>, value: T?): ZigToolchain

    fun workingDirectory(project: Project? = null): Path?

    suspend fun patchCommandLine(commandLine: GeneralCommandLine, project: Project? = null): GeneralCommandLine

    fun pathToExecutable(toolName: String, project: Project? = null): Path

    data class Ref(
        @JvmField
        @Attribute
        val marker: String? = null,
        @JvmField
        @MapAnnotation(surroundWithTag = false)
        val data: Map<String, String>? = null,
    )
}