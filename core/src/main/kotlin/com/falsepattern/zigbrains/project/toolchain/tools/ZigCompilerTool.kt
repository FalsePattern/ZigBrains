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

package com.falsepattern.zigbrains.project.toolchain.tools

import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchain
import com.falsepattern.zigbrains.shared.psi.decodeTextContent
import com.falsepattern.zigbrains.zon.ZonLanguage
import com.falsepattern.zigbrains.zon.psi.ZonExpr
import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.childrenOfType
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.nio.file.Path

class ZigCompilerTool(toolchain: ZigToolchain) : ZigTool(toolchain) {
    override val toolName: String
        get() = "zig"

    fun path(): Path {
        return toolchain.pathToExecutable(toolName)
    }

    suspend fun getEnv(project: Project?): Result<ZigToolchainEnvironmentSerializable> {
        val stdout = callWithArgs(toolchain.workingDirectory(project), "env").getOrElse { throwable -> return Result.failure(throwable) }.stdout
        return try {
            Result.success(envJson.decodeFromString<ZigToolchainEnvironmentSerializable>(stdout))
        } catch (e: SerializationException) {
            //Try parsing as zon
            try {
                val theMap = readAction {
                    val psi = PsiFileFactory.getInstance(project ?: ProjectManager.getInstance().defaultProject).createFileFromText(ZonLanguage, stdout)
                    val rootList = psi.childrenOfType<ZonExpr>().firstOrNull()?.initList?.fieldInitList ?: throw IllegalArgumentException()
                    val theMap = HashMap<String, String>()
                    for (field in rootList) {
                        val value = field.expr.stringLiteral?.decodeTextContent() ?: continue
                        val ident = field.identifier.text
                        theMap[ident] = value
                    }
                    theMap
                }
                val deserialized = ZigToolchainEnvironmentSerializable(
                    theMap.getZonOrThrow("zig_exe"),
                    theMap.getZonOrThrow("std_dir"),
                    theMap.getZonOrThrow("global_cache_dir"),
                    theMap.getZonOrThrow("lib_dir"),
                    theMap.getZonOrThrow("version"),
                    theMap.getZonOrThrow("target")
                )
                Result.success(deserialized)
            } catch (e2: Exception) {
                val e3 = IllegalStateException("could not deserialize zig env", e2)
                e3.addSuppressed(e)
                Result.failure(e3)
            }
        }
    }
}

private fun Map<String, String>.getZonOrThrow(index: String): String {
    return this[index] ?: throw IllegalArgumentException("Missing ZON element \"$index\"")
}

private val envJson = Json {
    ignoreUnknownKeys = true
}