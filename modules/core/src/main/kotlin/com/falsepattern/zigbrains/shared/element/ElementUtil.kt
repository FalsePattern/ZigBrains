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

package com.falsepattern.zigbrains.shared.element

import org.jdom.Element


fun Element.readString(name: String): String? {
    return children
        .firstOrNull { it.name == "ZigBrainsOption" && it.getAttributeValue("name") == name }
        ?.getAttributeValue("value")
}

fun Element.readBoolean(name: String): Boolean? {
    return readString(name)?.toBooleanStrictOrNull()
}

inline fun <reified T: Enum<T>> Element.readEnum(name: String): T? {
    return readEnum(name, T::class.java)
}

fun <T: Enum<T>> Element.readEnum(name: String, klass: Class<T>): T? {
    return readString(name)?.let { valueOf(it, klass) }
}

fun Element.readChild(name: String): Element? {
    return children
        .firstOrNull { it.name == "ZigBrainsNestedOption" && it.getAttributeValue("name") == name }
}

fun Element.readStrings(name: String): List<String>? {
    return children
        .firstOrNull { it.name == "ZigBrainsArrayOption" && it.getAttributeValue("name") == name }
        ?.children
        ?.mapNotNull { if (it.name == "ZigBrainsArrayEntry") it.getAttributeValue("value") else null }
}

fun Element.writeString(name: String, value: String) {
    val option = Element("ZigBrainsOption")
    option.setAttribute("name", name)
    option.setAttribute("value", value)

    addContent(option)
}

fun Element.writeBoolean(name: String, value: Boolean) {
    writeString(name, value.toString())
}

fun <T: Enum<T>> Element.writeEnum(name: String, value: T) {
    writeString(name, value.name)
}

fun Element.writeStrings(name: String, values: List<String>) {
    val arr = Element("ZigBrainsArrayOption")
    arr.setAttribute("name", name)
    for (value in values) {
        val subElem = Element("ZigBrainsArrayEntry")
        subElem.setAttribute("value", value)
        arr.addContent(subElem)
    }
    addContent(arr)
}

fun Element.writeChild(name: String): Element {
    val child = Element("ZigBrainsNestedOption")
    child.setAttribute("name", name)
    addContent(child)
    return child
}

private fun <T: Enum<T>> valueOf(type: String, klass: Class<T>): T? {
    return try {
        java.lang.Enum.valueOf(klass, type)
    } catch (e: IllegalArgumentException) {
        null
    }
}
