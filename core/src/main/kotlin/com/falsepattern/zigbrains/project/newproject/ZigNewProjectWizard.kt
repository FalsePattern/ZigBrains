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
import com.falsepattern.zigbrains.project.module.ZigModuleBuilder
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.GitNewProjectWizardData.Companion.gitData
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.language.LanguageGeneratorNewProjectWizard
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import javax.swing.Icon

class ZigNewProjectWizard: LanguageGeneratorNewProjectWizard {
    override val name: String
        get() = "Zig"
    override val icon: Icon
        get() = Icons.ZIG
    override val ordinal: Int
        get() = 900

    override fun createStep(parent: NewProjectWizardStep): NewProjectWizardStep {
        return ZigNewProjectWizardStep(parent)
    }

    private class ZigNewProjectWizardStep(parentStep: NewProjectWizardStep): AbstractNewProjectWizardStep(parentStep) {
        private val peer = ZigProjectGeneratorPeer(false)

        override fun setupUI(builder: Panel): Unit = with(builder) {
            row {
                cell(peer.myComponent).align(AlignX.FILL)
            }
        }

        override fun setupProject(project: Project) {
            val builder = ZigModuleBuilder()
            builder.configurationData = peer.settings
            val gitData = gitData
            builder.forceGitignore = gitData != null && gitData.git
            builder.commit(project)
        }
    }
}