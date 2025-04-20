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

package com.falsepattern.zigbrains.project.execution.run

import com.falsepattern.zigbrains.project.execution.base.ZigConfigProducer
import com.falsepattern.zigbrains.project.execution.base.findBuildZig
import com.falsepattern.zigbrains.project.execution.base.hasMainFunction
import com.falsepattern.zigbrains.project.execution.firstConfigFactory
import com.falsepattern.zigbrains.zig.psi.ZigFile
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import java.nio.file.Path

class ZigConfigProducerRun: ZigConfigProducer<ZigExecConfigRun>() {
    override fun getConfigurationFactory(): ConfigurationFactory {
        return firstConfigFactory<ZigConfigTypeRun>()
    }

    override fun setupConfigurationFromContext(configuration: ZigExecConfigRun, element: PsiElement, psiFile: ZigFile, filePath: Path, theFile: VirtualFile): Boolean {
        if (!psiFile.hasMainFunction()) {
            return false
        }
        if (theFile.findBuildZig() != null) {
            return false
        }
        configuration.filePath.path = filePath
        configuration.name = theFile.presentableName
        return true
    }

    override fun isConfigurationFromContext(configuration: ZigExecConfigRun, element: PsiElement, psiFile: ZigFile, filePath: Path, theFile: VirtualFile): Boolean {
        return filePath == configuration.filePath.path
    }

    override fun shouldReplace(self: ConfigurationFromContext, other: ConfigurationFromContext): Boolean {
        return self.configurationType is ZigConfigTypeRun
    }
}
