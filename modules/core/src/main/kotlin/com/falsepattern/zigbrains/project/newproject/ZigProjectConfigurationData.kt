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

package com.falsepattern.zigbrains.project.newproject

import com.falsepattern.zigbrains.lsp.settings.ZLSSettings
import com.falsepattern.zigbrains.lsp.settings.zlsSettings
import com.falsepattern.zigbrains.project.settings.ZigProjectSettings
import com.falsepattern.zigbrains.project.settings.zigProjectSettings
import com.falsepattern.zigbrains.project.template.ZigInitTemplate
import com.falsepattern.zigbrains.project.template.ZigProjectTemplate
import com.falsepattern.zigbrains.shared.coroutine.withEDTContext
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.GitRepositoryInitializer
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.util.ResourceUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@JvmRecord
data class ZigProjectConfigurationData(
    val git: Boolean,
    val projConf: ZigProjectSettings,
    val zlsConf: ZLSSettings,
    val selectedTemplate: ZigProjectTemplate
) {
    suspend fun generateProject(requestor: Any, project: Project, baseDir: VirtualFile, forceGitignore: Boolean): Boolean {
        withEDTContext {
            project.zigProjectSettings.loadState(projConf)
            project.zlsSettings.loadState(zlsConf)
        }

        val template = selectedTemplate

        if (template is ZigInitTemplate) {
            val toolchain = projConf.toolchain ?: run {
                Notification(
                    "zigbrains",
                    "Tried to generate project with zig init, but zig toolchain is invalid",
                    NotificationType.ERROR
                ).notify(project)
                return false
            }
            val zig = toolchain.zig
            val workDir = baseDir.toNioPathOrNull() ?: run {
                Notification(
                    "zigbrains",
                    "Tried to generate project with zig init, but base directory is invalid",
                    NotificationType.ERROR
                ).notify(project)
                return false
            }
            val result = zig.callWithArgs(workDir, "init")
            if (result.exitCode != 0) {
                Notification(
                    "zigbrains",
                    "\"zig init\" failed with exit code ${result.exitCode}! Check the IDE log files!",
                    NotificationType.ERROR
                )
                System.err.println(result.stderr)
                return false
            }
        } else {
            val projectName = project.name
            for (fileTemplate in template.fileTemplates()) {
                val (fileName, parentDir) = fileTemplate.key.let {
                    if (it.contains("/")) {
                        val slashIndex = it.indexOf("/")
                        val parentDir = withEDTContext {
                            baseDir.createChildDirectory(requestor, it.substring(0, slashIndex))
                        }
                        Pair(it.substring(slashIndex + 1), parentDir)
                    } else {
                        Pair(it, baseDir)
                    }
                }
                val templateDir = fileTemplate.value
                val resourceData = getResourceString("project-gen/$templateDir/$fileName.template")
                    ?.replace("@@PROJECT_NAME@@", projectName)
                    ?: continue
                withEDTContext {
                    val targetFile = parentDir.createChildData(requestor, fileName)
                    VfsUtil.saveText(targetFile, resourceData)
                }
            }
        }

        if (git) {
            withContext(Dispatchers.IO) {
                GitRepositoryInitializer.getInstance()?.initRepository(project, baseDir)
            }
        }

        if (git || forceGitignore) {
            createGitIgnoreFile(baseDir, requestor)
        }

        return true
    }

}

private suspend fun createGitIgnoreFile(projectDir: VirtualFile, requestor: Any) {
    if (projectDir.findChild(".gitignore") != null) {
        return
    }

    withContext(Dispatchers.IO) {
        ZigProjectConfigurationData::class.java.getResourceAsStream("/fileTemplates/internal/gitignore")?.use {
            val file = projectDir.createChildData(requestor, ".gitignore")
            file.setCharset(Charsets.UTF_8)
            file.setBinaryContent(it.readAllBytes())
        }
    }
}

private fun getResourceString(path: String): String? {
    return ResourceUtil.getResourceAsBytes(path, ZigProjectConfigurationData::class.java.classLoader)?.decodeToString()
}