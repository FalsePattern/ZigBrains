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

import com.falsepattern.zigbrains.lsp.zls.ZLSConfigurable
import com.falsepattern.zigbrains.lsp.zls.ZLSVersion
import com.falsepattern.zigbrains.lsp.zls.zlsInstallations
import com.falsepattern.zigbrains.shared.UUIDMapSerializable
import com.falsepattern.zigbrains.shared.ui.ListElem
import com.falsepattern.zigbrains.shared.ui.ListElemIn
import com.falsepattern.zigbrains.shared.ui.UUIDComboBoxDriver
import com.falsepattern.zigbrains.shared.ui.ZBComboBox
import com.falsepattern.zigbrains.shared.ui.ZBContext
import com.falsepattern.zigbrains.shared.ui.ZBModel
import com.intellij.openapi.ui.NamedConfigurable
import java.awt.Component
import java.util.UUID

object ZLSDriver: UUIDComboBoxDriver<ZLSVersion> {
    override val theMap: UUIDMapSerializable.Converting<ZLSVersion, *, *>
        get() = zlsInstallations

    override fun constructModelList(): List<ListElemIn<ZLSVersion>> {
        return ListElem.fetchGroup()
    }

    override fun createContext(model: ZBModel<ZLSVersion>): ZBContext<ZLSVersion> {
        return ZLSContext(null, model)
    }

    override fun createComboBox(model: ZBModel<ZLSVersion>): ZBComboBox<ZLSVersion> {
        return ZLSComboBox(model)
    }

    override suspend fun resolvePseudo(
        context: Component,
        elem: ListElem.Pseudo<ZLSVersion>
    ): UUID? {
        //TODO
        return null
    }

    override fun createNamedConfigurable(uuid: UUID, elem: ZLSVersion): NamedConfigurable<UUID> {
        //TODO
        return ZLSConfigurable(uuid, elem)
    }
}