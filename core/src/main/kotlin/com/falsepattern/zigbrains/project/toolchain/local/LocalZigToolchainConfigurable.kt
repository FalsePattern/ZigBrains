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

import com.falsepattern.zigbrains.project.settings.ZigProjectConfigurationProvider
import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchainConfigurable
import java.util.*

class LocalZigToolchainConfigurable(
    uuid: UUID,
    toolchain: LocalZigToolchain,
    data: ZigProjectConfigurationProvider.IUserDataBridge?,
    modal: Boolean
): ZigToolchainConfigurable<LocalZigToolchain>(uuid, toolchain, data, modal) {
    override fun createPanel() = LocalZigToolchainPanel()

    override fun setDisplayName(name: String?) {
        toolchain = toolchain.copy(name = name)
    }
}