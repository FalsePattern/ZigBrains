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

package com.falsepattern.zigbrains.project.newproject

import com.falsepattern.zigbrains.project.settings.ZigProjectConfigurationProvider
import com.falsepattern.zigbrains.project.template.ZigInitTemplate
import com.falsepattern.zigbrains.project.template.ZigProjectTemplate
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.GitRepositoryInitializer
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.platform.util.progress.reportProgress
import com.intellij.util.ResourceUtil
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import kotlinx.coroutines.launch

@JvmRecord
data class ZigProjectConfigurationData(
    val git: Boolean,
    val conf: List<ZigProjectConfigurationProvider.Settings>,
    val selectedTemplate: ZigProjectTemplate
) {
    @RequiresBackgroundThread
    suspend fun generateProject(requestor: Any, project: Project, baseDir: VirtualFile, forceGitignore: Boolean): Boolean {
        return reportProgress { reporter ->
            conf.forEach { it.apply(project) }

            val template = selectedTemplate

            if (!reporter.indeterminateStep("Initializing project") {
                if (template is ZigInitTemplate) {
                    val toolchain = conf
                        .mapNotNull { it as? ZigProjectConfigurationProvider.ToolchainProvider }
                        .firstNotNullOfOrNull { it.toolchain } ?: run {
                        Notification(
                            "zigbrains",
                            "Tried to generate project with zig init, but zig toolchain is invalid",
                            NotificationType.ERROR
                        ).notify(project)
                        return@indeterminateStep false
                    }
                    val zig = toolchain.zig
                    val workDir = baseDir.toNioPathOrNull() ?: run {
                        Notification(
                            "zigbrains",
                            "Tried to generate project with zig init, but base directory is invalid",
                            NotificationType.ERROR
                        ).notify(project)
                        return@indeterminateStep false
                    }
                    val result = zig.callWithArgs(workDir, "init").getOrElse { throwable ->
                        Notification(
                            "zigbrains",
                            "Failed to run \"zig init\": ${throwable.message}",
                            NotificationType.ERROR
                        ).notify(project)
                        return@indeterminateStep false
                    }
                    if (result.exitCode != 0) {
                        Notification(
                            "zigbrains",
                            "\"zig init\" failed with exit code ${result.exitCode}! Check the IDE log files!",
                            NotificationType.ERROR
                        ).notify(project)
                        System.err.println(result.stderr)
                        return@indeterminateStep false
                    }
                    return@indeterminateStep true
                } else {
                    writeAction {
                        val projectName = project.name
                        for (fileTemplate in template.fileTemplates()) {
                            val (fileName, parentDir) = fileTemplate.key.let {
                                if (it.contains("/")) {
                                    val slashIndex = it.indexOf("/")
                                    val parentDir = baseDir.createChildDirectory(requestor, it.substring(0, slashIndex))
                                    Pair(it.substring(slashIndex + 1), parentDir)
                                } else {
                                    Pair(it, baseDir)
                                }
                            }
                            val templateDir = fileTemplate.value
                            val resourceData = getResourceString("project-gen/$templateDir/$fileName.template")
                                                   ?.replace("@@PROJECT_NAME@@", projectName)
                                               ?: continue
                            val targetFile = parentDir.createChildData(requestor, fileName)
                            VfsUtil.saveText(targetFile, resourceData)
                        }
                    }
                    return@indeterminateStep true
                }
            }) return@reportProgress false

            if (git) {
                project.zigCoroutineScope.launch {
                    GitRepositoryInitializer.getInstance()?.initRepository(project, baseDir)
                    createGitIgnoreFile(project, baseDir, requestor)
                }
            } else if (forceGitignore) {
                createGitIgnoreFile(project, baseDir, requestor)
            }

            return@reportProgress true
        }
    }

}

private suspend fun createGitIgnoreFile(project: Project, projectDir: VirtualFile, requestor: Any) {
    if (projectDir.findChild(".gitignore") != null) {
        return
    }

    writeAction {
        ZigProjectConfigurationData::class.java.getResourceAsStream("/fileTemplates/internal/gitignore")?.use {
            val file = projectDir.createChildData(requestor, ".gitignore")
            file.charset = Charsets.UTF_8
            file.setBinaryContent(it.readAllBytes())
        }
    }
}

private fun getResourceString(path: String): String? {
    return ResourceUtil.getResourceAsBytes(path, ZigProjectConfigurationData::class.java.classLoader)?.decodeToString()
}