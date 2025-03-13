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

package com.falsepattern.zigbrains.lsp.settings

import com.falsepattern.zigbrains.shared.SubConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.Panel

class ZLSSettingsConfigurable(private val project: Project): SubConfigurable {
    private var appSettingsComponent: ZLSSettingsPanel? = null
    override fun createComponent(panel: Panel) {
        appSettingsComponent = ZLSSettingsPanel(project).apply { attach(panel) }.also { Disposer.register(this, it) }
    }

    override fun isModified(): Boolean {
        val data = appSettingsComponent?.data ?: return false
        return project.zlsSettings.state != data
    }

    override fun apply() {
        val data = appSettingsComponent?.data ?: return
        val settings = project.zlsSettings
        settings.state = data
    }

    override fun reset() {
        appSettingsComponent?.data = project.zlsSettings.state
    }

    override fun dispose() {
        appSettingsComponent = null
    }
}