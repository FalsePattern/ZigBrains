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

package com.falsepattern.zigbrains.project.execution.test

import com.falsepattern.zigbrains.ZigBrainsBundle
import com.falsepattern.zigbrains.project.execution.base.ZigConfigProducer
import com.falsepattern.zigbrains.project.execution.firstConfigFactory
import com.falsepattern.zigbrains.zig.psi.ZigContainerMembers
import com.falsepattern.zigbrains.zig.psi.ZigFile
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.util.childrenOfType
import java.nio.file.Path

class ZigConfigProducerTest: ZigConfigProducer<ZigExecConfigTest>() {
    override fun getConfigurationFactory(): ConfigurationFactory {
        return firstConfigFactory<ZigConfigTypeTest>()
    }

    override fun setupConfigurationFromContext(configuration: ZigExecConfigTest, element: PsiElement, psiFile: ZigFile, filePath: Path, theFile: VirtualFile): Boolean {
        val members = psiFile.childrenOfType<ZigContainerMembers>().firstOrNull() ?: return false
        if (members.containerDeclarationList.none { it.testDecl != null }) {
            return false
        }
        configuration.filePath.path = filePath
        configuration.name = ZigBrainsBundle.message("configuration.test.marker-name", theFile.presentableName)
        return true
    }

    override fun isConfigurationFromContext(configuration: ZigExecConfigTest, element: PsiElement, psiFile: ZigFile, filePath: Path, theFile: VirtualFile): Boolean {
        return filePath == configuration.filePath.path
    }

    override fun shouldReplace(self: ConfigurationFromContext, other: ConfigurationFromContext): Boolean {
        return self.configurationType is ZigConfigTypeTest
    }
}