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

package com.falsepattern.zigbrains.shared.cli

import com.falsepattern.zigbrains.ZigBrainsBundle
import com.intellij.openapi.options.ConfigurationException
import java.util.*


//From Apache Ant
/**
 * Crack a command line.
 * @param toProcess the command line to process.
 * @return the command line broken into strings.
 * An empty or null toProcess parameter results in a zero sized array.
 */
@Throws(ConfigurationException::class)
fun translateCommandline(toProcess: String): List<String> {
    if (toProcess.isEmpty()) {
        //no command? no string
        return listOf()
    }

    // parse with a simple finite state machine
    val normal = 0
    val inQuote = 1
    val inDoubleQuote = 2
    var state = normal
    val tok = StringTokenizer(toProcess, "\"' ", true)
    val result = ArrayList<String>()
    val current = StringBuilder()
    var lastTokenHasBeenQuoted = false

    while (tok.hasMoreTokens()) {
        val nextTok: String = tok.nextToken()
        when (state) {
            inQuote -> if ("'" == nextTok) {
                lastTokenHasBeenQuoted = true
                state = normal
            } else {
                current.append(nextTok)
            }

            inDoubleQuote -> if ("\"" == nextTok) {
                lastTokenHasBeenQuoted = true
                state = normal
            } else {
                current.append(nextTok)
            }

            else -> {
                if ("'" == nextTok) {
                    state = inQuote
                } else if ("\"" == nextTok) {
                    state = inDoubleQuote
                } else if (" " == nextTok) {
                    if (lastTokenHasBeenQuoted || current.isNotEmpty()) {
                        result.add(current.toString())
                        current.setLength(0)
                    }
                } else {
                    current.append(nextTok)
                }
                lastTokenHasBeenQuoted = false
            }
        }
    }
    if (lastTokenHasBeenQuoted || current.isNotEmpty()) {
        result.add(current.toString())
    }
    if (state == inQuote || state == inDoubleQuote) {
        throw ConfigurationException(ZigBrainsBundle.message("exception.translate-command-line.unbalanced-quotes", toProcess))
    }
    return result
}

fun coloredCliFlags(colored: Boolean, debug: Boolean): List<String> {
    return if (debug) {
        emptyList()
    } else {
        listOf("--color", if (colored) "on" else "off")
    }
}