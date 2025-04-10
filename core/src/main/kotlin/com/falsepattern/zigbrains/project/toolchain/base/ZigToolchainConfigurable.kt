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

import com.falsepattern.zigbrains.project.toolchain.ZigToolchainListService
import com.intellij.openapi.ui.NamedConfigurable
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.util.minimumWidth
import java.util.UUID
import javax.swing.JComponent

abstract class ZigToolchainConfigurable<T: ZigToolchain>(
    val uuid: UUID,
    tc: T
): NamedConfigurable<UUID>() {
    var toolchain: T = tc
        set(value) {
            ZigToolchainListService.getInstance().setToolchain(uuid, value)
            field = value
        }
    private var myView: ZigToolchainPanel<T>? = null

    abstract fun createPanel(): ZigToolchainPanel<T>

    override fun createOptionsPanel(): JComponent? {
        var view = myView
        if (view == null) {
            view = createPanel()
            view.reset(toolchain)
            myView = view
        }
        return panel {
            view.attach(this)
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
        return myView?.isModified(toolchain) == true
    }

    override fun apply() {
        myView?.apply(toolchain)?.let { toolchain = it }
    }

    override fun reset() {
        myView?.reset(toolchain)
    }

    override fun disposeUIResources() {
        myView?.dispose()
        myView = null
        super.disposeUIResources()
    }
}