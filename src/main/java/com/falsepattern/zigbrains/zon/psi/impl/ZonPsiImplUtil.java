/*
 * Copyright 2023 FalsePattern
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

package com.falsepattern.zigbrains.zon.psi.impl;

import com.falsepattern.zigbrains.zon.psi.ZonIdentifier;
import com.falsepattern.zigbrains.zon.psi.ZonStruct;
import com.intellij.psi.PsiElement;

import java.util.HashSet;
import java.util.Set;

public class ZonPsiImplUtil {
    public static Set<String> getKeys(ZonStruct struct) {
        var result = new HashSet<String>();
        for (var property: struct.getPropertyList()) {
            result.add(getText(property.getIdentifier()));
        }
        return result;
    }

    public static <T> T parent(PsiElement element, Class<T> parentType) {
        var parent = element.getParent();
        while (parent != null) {
            if (parentType.isInstance(parent)) {
                return parentType.cast(parent);
            }
            parent = parent.getParent();
        }
        return null;
    }

    public static String getText(ZonIdentifier identifier) {
        var idStr = identifier.getText();
        if (idStr.startsWith("@")) {
            return idStr.substring(2, idStr.length() - 2);
        } else {
            return idStr;
        }
    }
}
