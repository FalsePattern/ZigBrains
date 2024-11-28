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

import com.falsepattern.zigbrains.Icons
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.facet.ui.ValidationResult
import com.intellij.ide.util.projectWizard.AbstractNewProjectStep
import com.intellij.ide.util.projectWizard.CustomStepProjectGenerator
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.impl.welcomeScreen.AbstractActionWithPanel
import com.intellij.platform.DirectoryProjectGenerator
import com.intellij.platform.ProjectGeneratorPeer
import com.intellij.platform.ide.progress.withModalProgress
import kotlinx.coroutines.launch
import javax.swing.Icon

class ZigDirectoryProjectGenerator: DirectoryProjectGenerator<ZigProjectConfigurationData>, CustomStepProjectGenerator<ZigProjectConfigurationData> {
    override fun getName(): String {
        return "Zig"
    }

    override fun getLogo(): Icon {
        return Icons.Zig
    }

    override fun createPeer(): ProjectGeneratorPeer<ZigProjectConfigurationData> {
        return ZigProjectGeneratorPeer(true)
    }

    override fun validate(baseDirPath: String): ValidationResult {
        return ValidationResult.OK
    }

    override fun generateProject(project: Project, baseDir: VirtualFile, settings: ZigProjectConfigurationData, module: Module) {
        project.zigCoroutineScope.launch {
            withModalProgress(project, "Generating Project") {
                settings.generateProject(this, project, baseDir, false)
            }
        }
    }

    override fun createStep(
        projectGenerator: DirectoryProjectGenerator<ZigProjectConfigurationData>?,
        callback: AbstractNewProjectStep.AbstractCallback<ZigProjectConfigurationData>?
    ): AbstractActionWithPanel {
        return ZigProjectSettingsStep(projectGenerator!!, callback)
    }
}