/*
 * This file is part of ZigBrains.
 *
 * Copyright (C) 2023-2024 FalsePattern
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

@file:Suppress("HardCodedStringLiteral")

package com.falsepattern.zigbrains.lsp.config

import kotlinx.serialization.SerialName
import org.jetbrains.annotations.NonNls

data class ZLSConfig(
    @SerialName("zig_exe_path") val zigExePath: @NonNls String? = null,
    @SerialName("zig_lib_path") val zigLibPath: @NonNls String? = null,
    @SerialName("enable_build_on_save") val buildOnSave: Boolean? = null,
    @SerialName("build_on_save_step") val buildOnSaveStep: @NonNls String? = null,
    @SerialName("dangerous_comptime_experiments_do_not_enable") val comptimeInterpreter: Boolean? = null,
    @SerialName("highlight_global_var_declarations") val globalVarDeclarations: Boolean? = null
) {
    infix fun merge(other: ZLSConfig): ZLSConfig {
        return ZLSConfig(
            zigExePath ?: other.zigExePath,
            zigLibPath ?: other.zigLibPath,
            buildOnSave ?: other.buildOnSave,
            buildOnSaveStep ?: other.buildOnSaveStep,
            comptimeInterpreter ?: other.comptimeInterpreter,
            globalVarDeclarations ?: other.globalVarDeclarations
        )
    }
}
