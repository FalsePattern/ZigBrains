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

import com.falsepattern.zigbrains.shared.SubConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.Panel

class ZigProjectConfigurable(private val project: Project): SubConfigurable {
    private var settingsPanel: ZigProjectSettingsPanel? = null
    override fun createComponent(holder: ZigProjectConfigurationProvider.SettingsPanelHolder, panel: Panel): ZigProjectConfigurationProvider.SettingsPanel {
        settingsPanel?.let { Disposer.dispose(it) }
        val sp = ZigProjectSettingsPanel(holder, project).apply { attach(panel) }.also { Disposer.register(this, it) }
        settingsPanel = sp
        return sp
    }

    override fun isModified(): Boolean {
        return project.zigProjectSettings.isModified(settingsPanel?.data ?: return false)
    }

    override fun apply() {
        val service = project.zigProjectSettings
        val data = settingsPanel?.data ?: return
        val modified = service.isModified(data)
        service.state = data
        if (modified) {
            ZigProjectConfigurationProvider.mainConfigChanged(project)
        }
    }

    override fun reset() {
        settingsPanel?.data = project.zigProjectSettings.state
    }

    override fun dispose() {
        settingsPanel = null
    }
}