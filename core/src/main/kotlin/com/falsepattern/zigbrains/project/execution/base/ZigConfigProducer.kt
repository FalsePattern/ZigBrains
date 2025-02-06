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

package com.falsepattern.zigbrains.project.execution.base

import com.falsepattern.zigbrains.zig.psi.ZigFile
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.psi.PsiElement
import java.nio.file.Path

abstract class ZigConfigProducer<T: ZigExecConfig<T>>: LazyRunConfigurationProducer<T>() {
    abstract override fun getConfigurationFactory(): ConfigurationFactory

    override fun setupConfigurationFromContext(configuration: T, context: ConfigurationContext, sourceElement: Ref<PsiElement>): Boolean {
        val element = context.location?.psiElement ?: return false
        val psiFile = element.containingFile as? ZigFile ?: return false
        val theFile = psiFile.virtualFile ?: return false
        val filePath = theFile.toNioPathOrNull() ?: return false
        return setupConfigurationFromContext(configuration, element, filePath, theFile)
    }

    override fun isConfigurationFromContext(configuration: T, context: ConfigurationContext): Boolean {
        val element = context.location?.psiElement ?: return false
        val psiFile = element.containingFile as? ZigFile ?: return false
        val theFile = psiFile.virtualFile ?: return false
        val filePath = theFile.toNioPathOrNull() ?: return false
        return isConfigurationFromContext(configuration, element, filePath, theFile)
    }

    /*
    TODO implement these
    @Override
    protected boolean setupConfigurationFromContext(@NotNull ZigExecConfigRun configuration, PsiElement element, String filePath, VirtualFile theFile) {
        if (PsiUtil.getElementType(element) == ZigTypes.KEYWORD_TEST) {
            configuration.command = "test " + filePath;
            configuration.setName("Test " + theFile.getPresentableName());
        } else if ("build.zig".equals(theFile.getName())) {
            configuration.command = "build";
            configuration.setName("Build");
        } else {
            configuration.extraArgs = filePath;
            configuration.setName(theFile.getPresentableName());
        }
        return true;
    }

    @Override
    protected boolean isConfigurationFromContext(@NotNull ZigExecConfigRun configuration, String filePath, VirtualFile vFile, PsiElement element) {
        if (!configuration.command.contains(filePath)) {
            return configuration.command.startsWith("build") && vFile.getName().equals("build.zig");
        }
        return (PsiUtil.getElementType(element) == ZigTypes.KEYWORD_TEST) == configuration.command.startsWith("test ");
    }
     */

    protected abstract fun setupConfigurationFromContext(configuration: T, element: PsiElement, filePath: Path, theFile: VirtualFile): Boolean
    protected abstract fun isConfigurationFromContext(configuration: T, element: PsiElement, filePath: Path, theFile: VirtualFile): Boolean
    abstract override fun shouldReplace(self: ConfigurationFromContext, other: ConfigurationFromContext): Boolean
}