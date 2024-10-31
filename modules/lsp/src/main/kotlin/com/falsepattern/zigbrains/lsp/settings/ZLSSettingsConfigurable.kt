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

package com.falsepattern.zigbrains.lsp.settings

import com.falsepattern.zigbrains.lsp.ZLSBundle
import com.falsepattern.zigbrains.shared.NestedConfigurable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class ZLSSettingsConfigurable(private val project: Project): NestedConfigurable {
    private var appSettingsComponent: ZLSSettingsPanel? = null
    override fun createComponent(panel: Panel) {
        appSettingsComponent = ZLSSettingsPanel(project).apply { attach(panel) }
    }

    override fun isModified(): Boolean {
        val data = appSettingsComponent?.data ?: return false
        return project.zlsSettings.state != data
    }

    override fun apply() {
        val data = appSettingsComponent?.data ?: return
        val settings = project.zlsSettings
        val reloadZLS = settings.isModified(data)
        settings.state = data
        if (reloadZLS) {
            TODO("Not yet implemented")
        }
    }

    override fun reset() {
        appSettingsComponent?.data = project.zlsSettings.state
    }

    override fun disposeUIResources() {
        appSettingsComponent?.dispose()
        appSettingsComponent = null
    }

    override fun getDisplayName(): String {
        return ZLSBundle.message("configurable.name.zls.settings")
    }
}