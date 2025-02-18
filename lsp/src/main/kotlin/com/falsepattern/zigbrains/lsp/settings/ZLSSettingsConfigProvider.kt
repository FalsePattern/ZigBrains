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

import com.falsepattern.zigbrains.lsp.config.ZLSConfig
import com.falsepattern.zigbrains.lsp.config.ZLSConfigProvider
import com.intellij.openapi.project.Project

class ZLSSettingsConfigProvider: ZLSConfigProvider {
    override fun getEnvironment(project: Project, previous: ZLSConfig): ZLSConfig {
        val state = project.zlsSettings.state
        return previous.copy(
            buildOnSave = state.buildOnSave,
            buildOnSaveStep = state.buildOnSaveStep,
            globalVarDeclarations = state.globalVarDeclarations,
            comptimeInterpreter = state.comptimeInterpreter
        )
    }
}