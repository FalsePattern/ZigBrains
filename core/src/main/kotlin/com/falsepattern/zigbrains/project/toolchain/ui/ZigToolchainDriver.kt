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
import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchain
import com.falsepattern.zigbrains.project.toolchain.base.createNamedConfigurable
import com.falsepattern.zigbrains.project.toolchain.base.suggestZigToolchains
import com.falsepattern.zigbrains.project.toolchain.zigToolchainList
import com.falsepattern.zigbrains.shared.ui.ListElem
import com.falsepattern.zigbrains.shared.ui.ListElemIn
import com.falsepattern.zigbrains.shared.ui.Separator
import com.falsepattern.zigbrains.shared.ui.UUIDComboBoxDriver
import com.falsepattern.zigbrains.shared.ui.ZBComboBox
import com.falsepattern.zigbrains.shared.ui.ZBContext
import com.falsepattern.zigbrains.shared.ui.ZBModel
import com.falsepattern.zigbrains.shared.ui.asActual
import com.falsepattern.zigbrains.shared.ui.asPending
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.NamedConfigurable
import com.intellij.openapi.util.UserDataHolder
import java.awt.Component
import java.util.UUID

sealed interface ZigToolchainDriver: UUIDComboBoxDriver<ZigToolchain> {
    override val theMap get() = zigToolchainList

    override fun createContext(model: ZBModel<ZigToolchain>): ZBContext<ZigToolchain> {
        return TCContext(null, model)
    }

    override fun createComboBox(model: ZBModel<ZigToolchain>): ZBComboBox<ZigToolchain> {
        return TCComboBox(model)
    }

    override suspend fun resolvePseudo(
        context: Component,
        elem: ListElem.Pseudo<ZigToolchain>
    ): UUID? {
        return ZigToolchainComboBoxHandler.onItemSelected(context, elem)
    }

    object ForList: ZigToolchainDriver {
        override fun constructModelList(): List<ListElemIn<ZigToolchain>> {
            val modelList = ArrayList<ListElemIn<ZigToolchain>>()
            modelList.addAll(ListElem.fetchGroup())
            modelList.add(Separator(ZigBrainsBundle.message("settings.toolchain.model.detected.separator"), true))
            modelList.add(suggestZigToolchains().asPending())
            return modelList
        }

        override fun createNamedConfigurable(
            uuid: UUID,
            elem: ZigToolchain
        ): NamedConfigurable<UUID> {
            return elem.createNamedConfigurable(uuid, ZigProjectConfigurationProvider.UserDataBridge())
        }
    }

    class ForSelector(val data: ZigProjectConfigurationProvider.IUserDataBridge): ZigToolchainDriver {
        override fun constructModelList(): List<ListElemIn<ZigToolchain>> {
            val modelList = ArrayList<ListElemIn<ZigToolchain>>()
            modelList.add(ListElem.None())
            modelList.addAll(zigToolchainList.map { it.asActual() }.sortedBy { it.instance.name })
            modelList.add(Separator("", true))
            modelList.addAll(ListElem.fetchGroup())
            modelList.add(Separator(ZigBrainsBundle.message("settings.toolchain.model.detected.separator"), true))
            modelList.add(suggestZigToolchains(data.getUserData(PROJECT_KEY), data).asPending())
            return modelList
        }

        override fun createNamedConfigurable(
            uuid: UUID,
            elem: ZigToolchain
        ): NamedConfigurable<UUID> {
            return elem.createNamedConfigurable(uuid, data)
        }
    }
}