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

package com.falsepattern.zigbrains.project.settings

import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchain
import com.falsepattern.zigbrains.shared.SubConfigurable
import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Panel

interface ZigProjectConfigurationProvider {
    fun handleMainConfigChanged(project: Project)
    fun createConfigurable(project: Project): SubConfigurable
    fun createNewProjectSettingsPanel(holder: SettingsPanelHolder): SettingsPanel?
    val priority: Int
    companion object {
        private val EXTENSION_POINT_NAME = ExtensionPointName.create<ZigProjectConfigurationProvider>("com.falsepattern.zigbrains.projectConfigProvider")
        fun mainConfigChanged(project: Project) {
            EXTENSION_POINT_NAME.extensionList.forEach { it.handleMainConfigChanged(project) }
        }
        fun createConfigurables(project: Project): List<SubConfigurable> {
            return EXTENSION_POINT_NAME.extensionList.sortedBy { it.priority }.map { it.createConfigurable(project) }
        }
        fun createNewProjectSettingsPanels(holder: SettingsPanelHolder): List<SettingsPanel> {
            return EXTENSION_POINT_NAME.extensionList.sortedBy { it.priority }.mapNotNull { it.createNewProjectSettingsPanel(holder) }
        }
    }
    interface SettingsPanel: Disposable {
        val data: Settings
        fun attach(p: Panel)
        fun direnvChanged(state: Boolean)
    }
    interface SettingsPanelHolder {
        val panels: List<SettingsPanel>
    }
    interface Settings {
        fun apply(project: Project)
    }
    interface ToolchainProvider {
        val toolchain: ZigToolchain?
    }
}