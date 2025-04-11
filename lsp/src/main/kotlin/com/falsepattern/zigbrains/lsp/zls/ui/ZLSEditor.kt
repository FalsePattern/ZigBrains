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

import com.falsepattern.zigbrains.lsp.ZLSStarter
import com.falsepattern.zigbrains.lsp.startLSP
import com.falsepattern.zigbrains.lsp.zls.ZLSVersion
import com.falsepattern.zigbrains.lsp.zls.withZLS
import com.falsepattern.zigbrains.lsp.zls.zlsUUID
import com.falsepattern.zigbrains.project.settings.ZigProjectConfigurationProvider
import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchain
import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchainExtensionsProvider
import com.falsepattern.zigbrains.project.toolchain.ui.ImmutableElementPanel
import com.falsepattern.zigbrains.shared.ui.UUIDMapSelector
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.openapi.util.Key
import com.intellij.ui.dsl.builder.Panel
import kotlinx.coroutines.launch

class ZLSEditor<T: ZigToolchain>(private val sharedState: ZigProjectConfigurationProvider.IUserDataBridge?):
    UUIDMapSelector<ZLSVersion>(ZLSDriver.ForSelector(sharedState)),
    ImmutableElementPanel<T>,
    ZigProjectConfigurationProvider.UserDataListener
{
    init {
        sharedState?.addUserDataChangeListener(this)
    }

    override fun onUserDataChanged(key: Key<*>) {
        zigCoroutineScope.launch { listChanged() }
    }

    override fun attach(panel: Panel): Unit = with(panel) {
        row("ZLS") {
            attachComboBoxRow(this)
        }
    }

    override fun isModified(toolchain: T): Boolean {
        return toolchain.zlsUUID != selectedUUID
    }

    override fun apply(toolchain: T): T {
        return toolchain.withZLS(selectedUUID)
    }

    override fun reset(toolchain: T) {
        selectedUUID = toolchain.zlsUUID
    }

    override fun dispose() {
        super.dispose()
        sharedState?.removeUserDataChangeListener(this)
    }

    class Provider: ZigToolchainExtensionsProvider {
        override fun <T : ZigToolchain> createExtensionPanel(sharedState: ZigProjectConfigurationProvider.IUserDataBridge?): ImmutableElementPanel<T>? {
            return ZLSEditor(sharedState)
        }

        override val index: Int
            get() = 100

    }
}