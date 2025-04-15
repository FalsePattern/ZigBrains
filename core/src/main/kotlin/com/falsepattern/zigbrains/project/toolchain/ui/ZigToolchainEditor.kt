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
import com.falsepattern.zigbrains.project.toolchain.base.PanelState
import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchain
import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchainConfigurable
import com.falsepattern.zigbrains.project.toolchain.base.createZigToolchainExtensionPanels
import com.falsepattern.zigbrains.project.toolchain.zigToolchainList
import com.falsepattern.zigbrains.shared.SubConfigurable
import com.falsepattern.zigbrains.shared.ui.UUIDMapSelector
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Key
import com.intellij.ui.dsl.builder.Panel
import kotlinx.coroutines.launch
import java.util.*
import java.util.function.Supplier

class ZigToolchainEditor(private val sharedState: ZigProjectConfigurationProvider.IUserDataBridge):
    UUIDMapSelector<ZigToolchain>(ZigToolchainDriver.ForSelector(sharedState)),
    SubConfigurable<Project>,
    ZigProjectConfigurationProvider.UserDataListener
{
    private var myViews: List<ImmutableElementPanel<ZigToolchain>> = emptyList()
    init {
        sharedState.putUserData(ZigToolchainConfigurable.TOOLCHAIN_KEY, Supplier{selectedUUID?.let { zigToolchainList[it] }})
        sharedState.addUserDataChangeListener(this)
    }

    override fun onUserDataChanged(key: Key<*>) {
        if (key == ZigToolchainConfigurable.TOOLCHAIN_KEY)
            return
        zigCoroutineScope.launch { listChanged() }
    }


    override fun attach(panel: Panel): Unit = with(panel) {
        row(ZigBrainsBundle.message(
            if (sharedState.getUserData(PROJECT_KEY)?.isDefault == true)
                "settings.toolchain.editor.toolchain-default.label"
            else
                "settings.toolchain.editor.toolchain.label")
        ) {
            attachComboBoxRow(this)
        }
        var views = myViews
        if (views.isEmpty()) {
            views = ArrayList<ImmutableElementPanel<ZigToolchain>>()
            views.addAll(createZigToolchainExtensionPanels(sharedState, PanelState.ProjectEditor))
            myViews = views
        }
        views.forEach { it.attach(panel) }
    }

    override fun onSelection(uuid: UUID?) {
        sharedState.putUserData(ZigToolchainConfigurable.TOOLCHAIN_KEY, Supplier{selectedUUID?.let { zigToolchainList[it] }})
        refreshViews(uuid)
    }

    private fun refreshViews(uuid: UUID?) {
        val toolchain = uuid?.let { zigToolchainList[it] }
        myViews.forEach { it.reset(toolchain) }
    }

    override fun isModified(context: Project): Boolean {
        if (isEmpty)
            return false
        val uuid = selectedUUID
        if (ZigToolchainService.getInstance(context).toolchainUUID != selectedUUID) {
            return true
        }
        if (uuid == null)
            return false
        val tc = zigToolchainList[uuid]
        if (tc == null)
            return false
        return myViews.any { it.isModified(tc) }
    }

    override fun apply(context: Project) {
        val uuid = selectedUUID
        ZigToolchainService.getInstance(context).toolchainUUID = uuid
        if (uuid == null)
            return
        val tc = zigToolchainList[uuid]
        if (tc == null)
            return
        val finalTc = myViews.fold(tc) { acc, view -> view.apply(acc) ?: acc }
        zigToolchainList[uuid] = finalTc
    }

    override fun reset(context: Project?) {
        val project = context ?: ProjectManager.getInstance().defaultProject
        val svc = ZigToolchainService.getInstance(project)
        val uuid = svc.toolchainUUID
        selectedUUID = uuid
        refreshViews(uuid)
    }

    override fun dispose() {
        super.dispose()
        sharedState.removeUserDataChangeListener(this)
        myViews.forEach { it.dispose() }
        myViews = emptyList()
    }

    override val newProjectBeforeInitSelector get() = true
    class Provider: ZigProjectConfigurationProvider {
        override fun create(sharedState: ZigProjectConfigurationProvider.IUserDataBridge): SubConfigurable<Project>? {
            return ZigToolchainEditor(sharedState).also { it.reset(sharedState.getUserData(PROJECT_KEY)) }
        }

        override val index: Int get() = 0
    }
}