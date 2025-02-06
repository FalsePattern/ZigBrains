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

import com.falsepattern.zigbrains.project.execution.base.ZigProfileState
import com.falsepattern.zigbrains.project.toolchain.AbstractZigToolchain
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.execution.configurations.GeneralCommandLine
import com.jetbrains.cidr.execution.Installer
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import java.io.File

class ZigDebugEmitBinaryInstaller<ProfileState: ZigProfileState<*>>(
    private val profileState: ProfileState,
    private val toolchain: AbstractZigToolchain,
    private val executableFile: File,
    private val exeArgs: List<String>
): Installer {
    override fun install(): GeneralCommandLine {
        val cfg = profileState.configuration
        val cli = GeneralCommandLine().withExePath(executableFile.absolutePath)
        cfg.workingDirectory.path?.let { x -> cli.withWorkingDirectory(x) }
        cli.addParameters(exeArgs)
        cli.withCharset(Charsets.UTF_8)
        cli.withRedirectErrorStream(true)
        return profileState.configuration.project.zigCoroutineScope.async{
            profileState.configuration.patchCommandLine(cli)
        }.asCompletableFuture().join()
    }

    override fun getExecutableFile(): File {
        return executableFile
    }
}