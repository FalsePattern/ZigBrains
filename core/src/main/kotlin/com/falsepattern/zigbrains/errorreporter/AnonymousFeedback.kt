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

import com.falsepattern.zigbrains.ZigBrains
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.util.net.HttpConfigurable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.apache.http.HttpStatus
import java.lang.RuntimeException
import java.net.HttpURLConnection

object AnonymousFeedback {
    private const val API_URL = "https://falsepattern.com/zigbrains/errorReport"

    @OptIn(ExperimentalSerializationApi::class)
    fun sendFeedback(payload: ErrorData): ErrorResponse {
        val connection = getConnection(API_URL)
        connection.connect()
        connection.outputStream.use {
            Json.encodeToStream(payload, it)
        }
        val responseCode = connection.responseCode
        if (responseCode != HttpStatus.SC_CREATED) {
            throw RuntimeException("Error report submission failure: $responseCode")
        }

        val json = Json.decodeFromStream<ErrorResponse>(connection.getInputStream())
        connection.disconnect()
        return json
    }

    private fun getConnection(url: String): HttpURLConnection {
        val connection = connect(url)
        connection.doOutput = true
        connection.setRequestProperty("User-Agent", userAgent)
        connection.setRequestProperty("Content-Type", "application/json")
        connection.requestMethod = "POST"

        return connection
    }

    private fun connect(url: String): HttpURLConnection {
        val connection = HttpConfigurable.getInstance().openHttpConnection(url)
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        return connection
    }


    private val userAgent: String by lazy {
        var agent = "ZigBrains"

        PluginManagerCore.getPlugin(ZigBrains.pluginId)?.let {
            agent = "${it.name} (${it.version})"
        }
        agent
    }
}