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

import com.falsepattern.zigbrains.project.toolchain.ZigToolchainService
import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchain
import com.falsepattern.zigbrains.project.toolchain.base.withExtraData
import com.falsepattern.zigbrains.shared.asString
import com.falsepattern.zigbrains.shared.asUUID
import com.intellij.openapi.project.Project
import java.util.*

fun <T: ZigToolchain> T.withZLS(uuid: UUID?): T {
    return withExtraData("zls_uuid", uuid?.asString())
}

val ZigToolchain.zlsUUID: UUID? get() {
    return extraData["zls_uuid"]?.asUUID()
}

val ZigToolchain.zls: ZLSVersion? get() {
    return zlsUUID?.let { zlsInstallations[it] }
}

val Project.zls: ZLSVersion? get() = ZigToolchainService.getInstance(this).toolchain?.zls
