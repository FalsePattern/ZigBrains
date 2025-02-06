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

package com.falsepattern.zigbrains.debugger.runner.base

import com.falsepattern.zigbrains.debugger.ZigDebugBundle
import com.falsepattern.zigbrains.project.execution.base.ZigProfileState
import com.falsepattern.zigbrains.project.toolchain.AbstractZigToolchain
import com.intellij.execution.ExecutionException
import com.intellij.openapi.util.io.FileUtil
import com.intellij.platform.util.progress.withProgressText
import com.intellij.util.containers.orNull
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import kotlin.io.path.absolutePathString
import kotlin.io.path.isExecutable

abstract class ZigDebugParametersEmitBinaryBase<ProfileState: ZigProfileState<*>>(
    driverConfiguration: DebuggerDriverConfiguration,
    toolchain: AbstractZigToolchain,
    profileState: ProfileState,
) : ZigDebugParametersBase<ProfileState>(driverConfiguration, toolchain, profileState), PreLaunchAware {
    @Volatile
    protected lateinit var executableFile: File
        private set

    @Throws(ExecutionException::class)
    private suspend fun compileExe(listener: PreLaunchProcessListener): File {
        val commandLine = profileState.getCommandLine(toolchain, true)
        val tmpDir = FileUtil.createTempDirectory("zigbrains_debug", "", true).toPath()

        val exe = tmpDir.resolve("executable")
        commandLine.addParameters("-femit-bin=${exe.absolutePathString()}")

        if (listener.executeCommandLineWithHook(commandLine))
            throw ExecutionException(ZigDebugBundle.message("debug.base.compile.failed.generic"))

        return withContext(Dispatchers.IO) {
            Files.list(tmpDir).use { stream ->
                 stream.filter { !it.fileName.endsWith(".o") }
                     .filter { it.isExecutable() }
                     .findFirst()
                     .map { it.toFile() }
                     .orNull()
            }
        } ?: throw ExecutionException(ZigDebugBundle.message("debug.base.compile.failed.no-exe"))
    }

    @Throws(ExecutionException::class)
    override suspend fun preLaunch(listener: PreLaunchProcessListener) {
        this.executableFile = withProgressText("Compiling executable") {
            compileExe(listener)
        }
    }
}