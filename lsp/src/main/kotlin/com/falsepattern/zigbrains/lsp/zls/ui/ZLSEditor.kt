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

package com.falsepattern.zigbrains.lsp.zls.ui

import com.falsepattern.zigbrains.lsp.zls.ZLSService
import com.falsepattern.zigbrains.lsp.zls.ZLSVersion
import com.falsepattern.zigbrains.project.settings.ZigProjectConfigurationProvider
import com.falsepattern.zigbrains.shared.SubConfigurable
import com.falsepattern.zigbrains.shared.ui.UUIDMapEditor
import com.falsepattern.zigbrains.shared.ui.UUIDMapSelector
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Key
import com.intellij.ui.dsl.builder.Panel
import kotlinx.coroutines.launch

class ZLSEditor(private var project: Project?,
                private val sharedState: ZigProjectConfigurationProvider.IUserDataBridge):
    UUIDMapSelector<ZLSVersion>(ZLSDriver),
    SubConfigurable<Project>,
    ZigProjectConfigurationProvider.UserDataListener
{
    init {
        sharedState.addUserDataChangeListener(this)
    }

    override fun onUserDataChanged(key: Key<*>) {
        zigCoroutineScope.launch { listChanged() }
    }

    override fun attach(panel: Panel): Unit = with(panel) {
        row("ZLS") {
            attachComboBoxRow(this)
        }
    }

    override fun isModified(context: Project): Boolean {
        return ZLSService.getInstance(context).zlsUUID != selectedUUID
    }

    override fun apply(context: Project) {
        ZLSService.getInstance(context).zlsUUID = selectedUUID
    }

    override fun reset(context: Project?) {
        val project = context ?: ProjectManager.getInstance().defaultProject
        selectedUUID = ZLSService.getInstance(project).zlsUUID
    }

    override fun dispose() {
        super.dispose()
        sharedState.removeUserDataChangeListener(this)
    }

    override val newProjectBeforeInitSelector: Boolean get() = true
    class Provider: ZigProjectConfigurationProvider {
        override fun create(
            project: Project?,
            sharedState: ZigProjectConfigurationProvider.IUserDataBridge
        ): SubConfigurable<Project>? {
            return ZLSEditor(project, sharedState)
        }

        override val index: Int
            get() = 50

    }
}