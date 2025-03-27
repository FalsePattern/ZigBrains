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

package com.falsepattern.zigbrains.project.module

import com.falsepattern.zigbrains.project.newproject.ZigProjectConfigurationData
import com.falsepattern.zigbrains.project.newproject.ZigProjectGeneratorPeer
import com.falsepattern.zigbrains.shared.coroutine.withEDTContext
import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.util.Disposer
import com.intellij.util.ui.JBUI
import javax.swing.JComponent

class ZigModuleBuilder: ModuleBuilder() {
    var configurationData: ZigProjectConfigurationData? = null
    var forceGitignore = false

    override fun getModuleType(): ModuleType<*> {
        return ZigModuleType
    }

    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
        super.setupRootModel(modifiableRootModel)
    }

    override fun getCustomOptionsStep(context: WizardContext?, parentDisposable: Disposable?): ModuleWizardStep? {
        val step = ZigModuleWizardStep(parentDisposable)
        parentDisposable?.let { Disposer.register(it, step.peer) }
        return step
    }

    suspend fun createProject(rootModel: ModifiableRootModel) {
        val contentEntry = doAddContentEntry(rootModel) ?: return
        val root = contentEntry.file ?: return
        val config = configurationData ?: return
        config.generateProject(this, rootModel.project, root, forceGitignore)
        withEDTContext(ModalityState.defaultModalityState()) {
            root.refresh(false, true)
        }
    }

    inner class ZigModuleWizardStep(parent: Disposable?): ModuleWizardStep() {
        internal val peer = ZigProjectGeneratorPeer(true).also { Disposer.register(parent ?: return@also, it) }

        override fun getComponent(): JComponent {
            return peer.myComponent.withBorder()
        }

        override fun disposeUIResources() {
            Disposer.dispose(peer)
        }

        override fun updateDataModel() {
            this@ZigModuleBuilder.configurationData = peer.settings
        }
    }
}

private fun <T: JComponent> T.withBorder(): T {
    border = JBUI.Borders.empty(14, 20)
    return this
}