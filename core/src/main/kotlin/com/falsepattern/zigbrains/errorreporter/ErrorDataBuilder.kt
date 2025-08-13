/*
 * ZigBrains
 * Copyright (C) 2023-2025 FalsePattern
 * All Rights Reserved
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.falsepattern.zigbrains.errorreporter

import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.ex.ApplicationInfoEx
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtil

class ErrorDataBuilder(var throwable: Throwable?, private val lastAction: String?) {

    var message: String? = null
        get() = field ?: throwable?.message

    var description: String? = null
    var pluginName: String? = null
    var pluginVersion: String? = null
    var attachments = emptyList<Attachment>()

    private val versionRegex by lazy(LazyThreadSafetyMode.NONE) {
        Regex("""(?<pluginVersion>\d+\.\d+\.\d+)-(?<intellijVersion>\d{3})""")
    }

    fun build(): ErrorData {
        val appInfo = ApplicationInfoEx.getInstanceEx()
        val namesInfo = ApplicationNamesInfo.getInstance()

        return ErrorData(
            description,
            pluginName,
            pluginVersion,
            SystemInfo.OS_NAME,
            SystemInfo.JAVA_VERSION,
            SystemInfo.JAVA_VENDOR,
            namesInfo.productName,
            namesInfo.fullProductName,
            appInfo.versionName,
            appInfo.isEAP,
            appInfo.build.asString(),
            appInfo.fullVersion,
            lastAction?.ifBlank { null },
            message,
            buildStacktrace(),
            buildAttachments()
        )
    }

    private fun buildAttachments(): List<ErrorData.Attachment> {
        return attachments.map { it -> ErrorData.Attachment(it.name, it.displayText, it.encodedBytes) }
    }

    private fun buildStacktrace(): ErrorData.StackTrace? {
        val t = throwable ?: return null
        val elements = ArrayList<ErrorData.StackTraceElement>()
        val visited = HashSet<Throwable>()
        val remaining = ArrayList<Throwable>()
        remaining.add(t)
        while (remaining.isNotEmpty()) {
            val t2 = remaining.removeLast()
            if (visited.contains(t2)) {
                continue
            }
            visited.add(t2)
            remaining.addAll(t2.suppressed)
            t2.cause?.let { remaining.add(it) }
            for (element in t2.stackTrace) {
                if (!element.className.startsWith("com.falsepattern.zigbrains")) {
                    continue
                }
                elements.add(ErrorData.StackTraceElement(element.className, element.fileName, element.lineNumber, element.toString()))
            }
        }
        val stackText = t.stackTraceToString()
        return ErrorData.StackTrace(stackText, elements)
    }

    private fun escape(text: Any) = StringUtil.escapeXmlEntities(text.toString())

    companion object {
        val submoduleMappings = mapOf(
            "com.falsepattern.zigbrains.clion" to "cidr",
            "com.falsepattern.zigbrains.debugbridge" to "cidr",
            "com.falsepattern.zigbrains.debugger" to "cidr",
            "com.falsepattern.zigbrains.lsp" to "lsp",
        )
    }
}