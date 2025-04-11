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

package com.falsepattern.zigbrains.project.toolchain.ui

import com.falsepattern.zigbrains.ZigBrainsBundle
import com.falsepattern.zigbrains.project.settings.ZigProjectConfigurationProvider
import com.falsepattern.zigbrains.project.settings.ZigProjectConfigurationProvider.Companion.PROJECT_KEY
import com.falsepattern.zigbrains.project.toolchain.ZigToolchainService
import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchain
import com.falsepattern.zigbrains.shared.SubConfigurable
import com.falsepattern.zigbrains.shared.ui.UUIDMapSelector
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Key
import com.intellij.ui.dsl.builder.Panel
import kotlinx.coroutines.launch

class ZigToolchainEditor(private val sharedState: ZigProjectConfigurationProvider.IUserDataBridge):
    UUIDMapSelector<ZigToolchain>(ZigToolchainDriver.ForSelector(sharedState)),
    SubConfigurable<Project>,
    ZigProjectConfigurationProvider.UserDataListener
{
    init {
        sharedState.addUserDataChangeListener(this)
    }

    override fun onUserDataChanged(key: Key<*>) {
        zigCoroutineScope.launch { listChanged() }
    }


    override fun attach(p: Panel): Unit = with(p) {
        row(ZigBrainsBundle.message(
            if (sharedState.getUserData(PROJECT_KEY)?.isDefault == true)
                "settings.toolchain.editor.toolchain-default.label"
            else
                "settings.toolchain.editor.toolchain.label")
        ) {
            attachComboBoxRow(this)
        }
    }

    override fun isModified(context: Project): Boolean {
        return ZigToolchainService.getInstance(context).toolchainUUID != selectedUUID
    }

    override fun apply(context: Project) {
        ZigToolchainService.getInstance(context).toolchainUUID = selectedUUID
    }

    override fun reset(context: Project?) {
        val project = context ?: ProjectManager.getInstance().defaultProject
        selectedUUID = ZigToolchainService.getInstance(project).toolchainUUID
    }

    override fun dispose() {
        super.dispose()
        sharedState.removeUserDataChangeListener(this)
    }

    override val newProjectBeforeInitSelector get() = true
    class Provider: ZigProjectConfigurationProvider {
        override fun create(sharedState: ZigProjectConfigurationProvider.IUserDataBridge): SubConfigurable<Project>? {
            return ZigToolchainEditor(sharedState).also { it.reset(sharedState.getUserData(PROJECT_KEY)) }
        }

        override val index: Int get() = 0
    }
}