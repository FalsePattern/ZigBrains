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

package com.falsepattern.zigbrains.lsp.settings

import org.jetbrains.annotations.NonNls

data class ZLSSettings(
    var direnv: Boolean = false,
    var zlsPath: @NonNls String = "",
    var zlsConfigPath: @NonNls String = "",
    var debug: Boolean = false,
    var messageTrace: Boolean = false,
    var buildOnSave: Boolean = false,
    var buildOnSaveStep: @NonNls String = "install",
    var globalVarDeclarations: Boolean = false,
    var comptimeInterpreter: Boolean = false,
    var inlayHints: Boolean = true,
    var inlayHintsCompact: Boolean = true
)
