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
import org.jetbrains.annotations.Nullable;

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

    public static void writeString(Element element, String name, String value) {
        val option = new Element("ZigBrainsOption");
        option.setAttribute("name", name);
        option.setAttribute("value", value);

        element.addContent(option);
    }

    public static void writeBoolean(Element element, String name, boolean state) {
        writeString(element, name, Boolean.toString(state));
    }

    public static Optional<Boolean> readBoolean(Element element, String name) {
        return readString(element, name).map(Boolean::parseBoolean);
    }
}
