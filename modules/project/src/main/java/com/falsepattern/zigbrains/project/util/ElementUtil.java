/*
 * Copyright 2023-2024 FalsePattern
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.falsepattern.zigbrains.project.util;

import lombok.val;
import org.jdom.Element;

import java.util.Objects;
import java.util.Optional;

public class ElementUtil {
    public static Optional<String> readString(Element element, String name) {
        return element.getChildren()
                      .stream()
                      .filter(it -> it.getName()
                                      .equals("ZigBrainsOption") &&
                                    it.getAttributeValue("name")
                                      .equals(name))
                      .findAny()
                      .map(it -> it.getAttributeValue("value"));
    }

    public static Optional<Boolean> readBoolean(Element element, String name) {
        return readString(element, name).map(Boolean::parseBoolean);
    }

    public static <T extends Enum<T>> Optional<T> readEnum(Element element, String name, Class<T> enumClass) {
        return readString(element, name).map(value -> {
            try {
                val field = enumClass.getDeclaredField(value);
                //noinspection unchecked
                return (T) field.get(null);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                return null;
            }
        });
    }

    public static Optional<Element> readChild(Element element, String name) {
        return element.getChildren()
                .stream()
                .filter(it -> it.getName()
                                .equals("ZigBrainsNestedOption") &&
                              it.getAttributeValue("name")
                                .equals(name))
                .findAny();
    }

    public static Optional<String[]> readStrings(Element element, String name) {
        return element.getChildren()
                .stream()
                .filter(it -> it.getName()
                                .equals("ZigBrainsArrayOption") &&
                              it.getAttributeValue("name")
                                .equals(name))
                .findAny()
                .map(it -> it.getChildren()
                        .stream()
                        .filter(it2 -> it2.getName()
                                          .equals("ZigBrainsArrayEntry"))
                        .map(it2 -> it2.getAttributeValue("value"))
                        .toArray(String[]::new));
    }

    public static void writeString(Element element, String name, String value) {
        val option = new Element("ZigBrainsOption");
        option.setAttribute("name", name);
        option.setAttribute("value", Objects.requireNonNullElse(value, ""));

        element.addContent(option);
    }

    public static void writeBoolean(Element element, String name, boolean state) {
        writeString(element, name, Boolean.toString(state));
    }

    public static <T extends Enum<T>> void writeEnum(Element element, String name, T value) {
        writeString(element, name, value.name());
    }

    public static void writeStrings(Element element, String name, String... values) {
        val arr = new Element("ZigBrainsArrayOption");
        arr.setAttribute("name", name);
        for (val value: values) {
            val subElem = new Element("ZigBrainsArrayEntry");
            subElem.setAttribute("value", value);
            arr.addContent(subElem);
        }
        element.addContent(arr);
    }

    public static Element writeChild(Element element, String name) {
        val child = new Element("ZigBrainsNestedOption");
        child.setAttribute("name", name);
        element.addContent(child);
        return child;
    }
}
