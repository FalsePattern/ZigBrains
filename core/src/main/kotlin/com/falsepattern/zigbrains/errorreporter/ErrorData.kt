/*
 * This file is part of ZigBrains.
 *
 * Copyright (C) 2023-2026 FalsePattern
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

package com.falsepattern.zigbrains.errorreporter

import kotlinx.serialization.Serializable

@Serializable
data class ErrorData(
    val description: String?,
    val pluginName: String?,
    val pluginVersion: String?,
    val osName: String,
    val javaVersion: String,
    val javaVmVendor: String,
    val appName: String,
    val appFullName: String,
    val appVersionName: String,
    val isEAP: Boolean,
    val appBuild: String,
    val appVersion: String,
    val lastAction: String?,
    val errorMessage: String?,
    val stackTrace: StackTrace?,
    val attachments: List<Attachment>
) {
    @Serializable
    data class StackTrace(
        val text: String,
        val elements: List<StackTraceElement>
    )

    @Serializable
    data class StackTraceElement(
        val className: String,
        val file: String?,
        val line: Int,
        val text: String,
    )
    @Serializable
    data class Attachment(
        val name: String,
        val displayText: String,
        val encodedBytes: String
    )
}