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

package com.falsepattern.zigbrains.debugger.runner.build

import com.falsepattern.zigbrains.debugger.ZigDebugBundle
import com.falsepattern.zigbrains.debugger.runner.base.PreLaunchAware
import com.falsepattern.zigbrains.debugger.runner.base.PreLaunchProcessListener
import com.falsepattern.zigbrains.debugger.runner.base.ZigDebugEmitBinaryInstaller
import com.falsepattern.zigbrains.debugger.runner.base.ZigDebugParametersBase
import com.falsepattern.zigbrains.project.execution.build.ZigProfileStateBuild
import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchain
import com.intellij.execution.ExecutionException
import com.intellij.openapi.util.SystemInfo
import com.intellij.platform.util.progress.withProgressText
import com.jetbrains.cidr.execution.Installer
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.PropertyKey
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.io.path.isExecutable
import kotlin.io.path.isRegularFile
import kotlin.io.path.pathString

class ZigDebugParametersBuild(
    driverConfiguration: DebuggerDriverConfiguration,
    toolchain: ZigToolchain,
    profileState: ZigProfileStateBuild
) : ZigDebugParametersBase<ZigProfileStateBuild>(driverConfiguration, toolchain, profileState), PreLaunchAware {
    @Volatile
    private lateinit var executableFile: File

    override fun getInstaller(): Installer {
        return ZigDebugEmitBinaryInstaller(profileState, toolchain, executableFile, profileState.configuration.exeArgs.argsSplit())
    }

    @Throws(ExecutionException::class)
    override suspend fun preLaunch(listener: PreLaunchProcessListener) {
        withProgressText("Building zig project") {
            withContext(Dispatchers.IO) {
                val commandLine = profileState.getCommandLine(toolchain, true)
                val cliStr = commandLine.commandLineString
                if (listener.executeCommandLineWithHook(profileState.environment.project, commandLine))
                    return@withContext

                val exe = profileState.configuration.exePath.path ?: fail("debug.build.compile.failed.no-exe-path")

                if (!exe.toFile().exists())
                    fail("debug.build.compile.failed.no-file", exe.pathString, cliStr)
                else if (!exe.isExecutable())
                    fail("debug.build.compile.failed.non-exec-file", exe)

                executableFile = exe.toFile()
            }
        }
    }

}

@Throws(ExecutionException::class)
private fun fail(@PropertyKey(resourceBundle = ZigDebugBundle.BUNDLE) messageKey: String, vararg params: Any): Nothing {
    throw ExecutionException(ZigDebugBundle.message(messageKey, *params))
}