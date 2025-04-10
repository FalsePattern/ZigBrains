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

import com.falsepattern.zigbrains.project.toolchain.zigToolchainList
import com.intellij.openapi.ui.NamedConfigurable
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.dsl.builder.panel
import java.util.UUID
import javax.swing.JComponent

abstract class ZigToolchainConfigurable<T: ZigToolchain>(
    val uuid: UUID,
    tc: T
): NamedConfigurable<UUID>() {
    var toolchain: T = tc
        set(value) {
            zigToolchainList[uuid] = value
            field = value
        }
    private var myViews: List<ZigToolchainPanel<T>> = emptyList()

    abstract fun createPanel(): ZigToolchainPanel<T>

    override fun createOptionsPanel(): JComponent? {
        var views = myViews
        if (views.isEmpty()) {
            views = ArrayList<ZigToolchainPanel<T>>()
            views.add(createPanel())
            views.addAll(createZigToolchainExtensionPanels())
            views.forEach { it.reset(toolchain) }
            myViews = views
        }
        return panel {
            views.forEach { it.attach(this@panel) }
        }.withMinimumWidth(20)
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
}