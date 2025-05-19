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

package com.falsepattern.zigbrains.debugger.dap

import com.intellij.openapi.util.Expirable
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.UserDataHolderEx
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration
import com.jetbrains.cidr.execution.debugger.backend.EvaluationContext
import com.jetbrains.cidr.execution.debugger.backend.LLFrame
import com.jetbrains.cidr.execution.debugger.backend.LLThread
import com.jetbrains.cidr.execution.debugger.backend.LLValue
import com.jetbrains.cidr.execution.debugger.backend.LLValueData
import org.eclipse.lsp4j.debug.InitializeRequestArguments

abstract class DAPDebuggerDriverConfiguration: DebuggerDriverConfiguration() {
    override fun createEvaluationContext(driver: DebuggerDriver, expirable: Expirable?, llThread: LLThread, llFrame: LLFrame, data: UserDataHolderEx): EvaluationContext {
        return object : EvaluationContext(driver, expirable, llThread, llFrame, data) {
            override fun convertToRValue(data: LLValueData, pair: Pair<LLValue, String>): String {
                return cast(pair.second, pair.first?.type)
            }
        }
    }

    abstract fun customizeInitializeArguments(initArgs: InitializeRequestArguments)
}