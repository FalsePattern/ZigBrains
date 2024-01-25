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

package com.falsepattern.zigbrains.common.util;

import com.intellij.psi.PsiElement;

import java.util.Optional;

public class PsiElementUtil {
    public static <T> Optional<T> parent(PsiElement element, Class<T> parentType) {
        if (element == null) {
            return Optional.empty();
        }
        var parent = element.getParent();
        while (parent != null) {
            if (parentType.isInstance(parent)) {
                return Optional.of(parentType.cast(parent));
            }
            parent = parent.getParent();
        }
        return Optional.empty();
    }
}
