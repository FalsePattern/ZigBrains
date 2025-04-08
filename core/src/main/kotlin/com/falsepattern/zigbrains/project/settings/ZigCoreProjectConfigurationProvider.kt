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

import com.falsepattern.zigbrains.project.toolchain.ui.ZigToolchainEditor
import com.falsepattern.zigbrains.shared.SubConfigurable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project

class ZigCoreProjectConfigurationProvider: ZigProjectConfigurationProvider {
    override fun handleMainConfigChanged(project: Project) {
    }

    override fun createConfigurable(project: Project): Configurable {
        return ZigToolchainEditor.Adapter(project)
    }

    override fun createNewProjectSettingsPanel(): SubConfigurable<Project> {
        return ZigToolchainEditor().also { it.reset(null) }
    }

    override val priority: Int
        get() = 0
}