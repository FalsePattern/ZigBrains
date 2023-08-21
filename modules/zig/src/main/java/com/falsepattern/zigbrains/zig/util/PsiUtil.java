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

package com.falsepattern.zigbrains.zig.util;

import com.falsepattern.zigbrains.zig.psi.ZigFnProto;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public class PsiUtil {
    @Contract("null -> null")
    public static @Nullable IElementType getElementType(@Nullable PsiElement element) {
        if (element instanceof ASTWrapperPsiElement ast) {
            return ast.getNode().getElementType();
        } else if (element instanceof ASTNode ast) {
            return ast.getElementType();
        }
        return null;
    }
}
