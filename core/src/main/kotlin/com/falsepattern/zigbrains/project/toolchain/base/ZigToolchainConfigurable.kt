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

package com.falsepattern.zigbrains.project.toolchain.base

import com.falsepattern.zigbrains.project.settings.ZigProjectConfigurationProvider
import com.falsepattern.zigbrains.project.toolchain.ui.ImmutableElementPanel
import com.falsepattern.zigbrains.project.toolchain.zigToolchainList
import com.intellij.openapi.ui.NamedConfigurable
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.dsl.builder.panel
import java.util.*
import java.util.function.Supplier
import javax.swing.JComponent

abstract class ZigToolchainConfigurable<T: ZigToolchain>(
    val uuid: UUID,
    tc: T,
    val data: ZigProjectConfigurationProvider.IUserDataBridge?,
    val modal: Boolean
): NamedConfigurable<UUID>() {
    var toolchain: T = tc
        set(value) {
            zigToolchainList[uuid] = value
            field = value
        }

    init {
        data?.putUserData(TOOLCHAIN_KEY, Supplier{toolchain})
    }
    private var myViews: List<ImmutableElementPanel<T>> = emptyList()

    abstract fun createPanel(): ImmutableElementPanel<T>

    override fun createOptionsPanel(): JComponent? {
        var views = myViews
        if (views.isEmpty()) {
            views = ArrayList<ImmutableElementPanel<T>>()
            views.add(createPanel())
            views.addAll(createZigToolchainExtensionPanels(data, if (modal) PanelState.ModalEditor else PanelState.ListEditor))
            myViews = views
        }
        val p = panel {
            views.forEach { it.attach(this@panel) }
        }.withMinimumWidth(20)
        views.forEach { it.reset(toolchain) }
        return p
    }

    override fun getEditableObject(): UUID? {
        return uuid
    }

    override fun getBannerSlogan(): @NlsContexts.DetailedDescription String? {
        return displayName
    }

    override fun getDisplayName(): @NlsContexts.ConfigurableName String? {
        return toolchain.name
    }

    override fun isModified(): Boolean {
        return myViews.any { it.isModified(toolchain) }
    }

    override fun apply() {
        toolchain = myViews.fold(toolchain) { tc, view -> view.apply(tc) ?: tc }
    }

    override fun reset() {
        myViews.forEach { it.reset(toolchain) }
    }

    override fun disposeUIResources() {
        myViews.forEach { it.dispose() }
        myViews = emptyList()
        super.disposeUIResources()
    }

    companion object {
        val TOOLCHAIN_KEY: Key<Supplier<ZigToolchain?>> = Key.create("TOOLCHAIN")
    }
}