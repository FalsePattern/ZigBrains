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

package com.falsepattern.zigbrains.lsp.zls

import com.intellij.openapi.ui.NamedConfigurable
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.dsl.builder.panel
import java.awt.Dimension
import java.util.*
import javax.swing.JComponent

class ZLSConfigurable(val uuid: UUID, zls: ZLSVersion): NamedConfigurable<UUID>() {
    var zls: ZLSVersion = zls
        set(value) {
            zlsInstallations[uuid] = value
            field = value
        }
    private var myView: ZLSPanel? = null

    override fun setDisplayName(name: String?) {
        zls = zls.copy(name = name)
    }

    override fun getEditableObject(): UUID? {
        return uuid
    }

    override fun getBannerSlogan(): @NlsContexts.DetailedDescription String? {
        return displayName
    }

    override fun createOptionsPanel(): JComponent? {
        var view = myView
        if (view == null) {
            view = ZLSPanel()
            view.reset(zls)
            myView = view
        }
        val p = panel {
            view.attach(this@panel)
        }
        p.preferredSize = Dimension(640, 480)
        return p
    }

    override fun getDisplayName(): @NlsContexts.ConfigurableName String? {
        return zls.name
    }

    override fun isModified(): Boolean {
        return myView?.isModified(zls) == true
    }

    override fun apply() {
        myView?.apply(zls)?.let { zls = it }
    }

    override fun reset() {
        myView?.reset(zls)
    }

    override fun disposeUIResources() {
        myView?.dispose()
        myView = null
        super.disposeUIResources()
    }
}