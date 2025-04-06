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

package com.falsepattern.zigbrains.project.toolchain.local

import com.falsepattern.zigbrains.project.toolchain.zigToolchainList
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.NamedConfigurable
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.dsl.builder.panel
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.swing.JComponent

class LocalZigToolchainConfigurable(
    val uuid: UUID,
    toolchain: LocalZigToolchain,
    private val project: Project
): NamedConfigurable<UUID>() {
    var toolchain: LocalZigToolchain = toolchain
        set(value) {
            zigToolchainList.setToolchain(uuid, value)
            field = value
        }
    private var myView: LocalZigToolchainPanel? = null
    override fun setDisplayName(name: String?) {
        toolchain = toolchain.copy(name = name)
    }

    override fun getEditableObject(): UUID {
        return uuid
    }

    override fun getBannerSlogan(): @NlsContexts.DetailedDescription String? {
        return displayName
    }

    override fun createOptionsPanel(): JComponent? {
        var view = myView
        if (view == null) {
            view = LocalZigToolchainPanel()
            view.reset(this)
            myView = view
        }
        return panel {
            view.attach(this)
        }
    }

    override fun getDisplayName(): @NlsContexts.ConfigurableName String? {
        var theName = toolchain.name
        if (theName == null) {
            val version = toolchain.zig.let { runBlocking { it.getEnv(project) } }.getOrNull()?.version
            if (version != null) {
                theName = "Zig $version"
                toolchain = toolchain.copy(name = theName)
            }
        }
        return theName
    }

    override fun isModified(): Boolean {
        return myView?.isModified(this) == true
    }

    override fun apply() {
        myView?.apply(this)
    }

    override fun reset() {
        myView?.reset(this)
    }

    override fun disposeUIResources() {
        super.disposeUIResources()
    }
}