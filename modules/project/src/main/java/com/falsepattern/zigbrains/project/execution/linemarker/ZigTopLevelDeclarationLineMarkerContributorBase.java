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

package com.falsepattern.zigbrains.project.execution.linemarker;

import com.falsepattern.zigbrains.zig.psi.ZigTypes;
import com.falsepattern.zigbrains.zig.util.PsiUtil;
import com.intellij.execution.lineMarker.ExecutorAction;
import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public abstract class ZigTopLevelDeclarationLineMarkerContributorBase extends RunLineMarkerContributor {
    @Override
    public @Nullable Info getInfo(@NotNull PsiElement element) {
        PsiElement parent = getDeclaration(element);

        int nestingLevel = 0;
        while (parent != null && !(parent instanceof PsiFile)) {
            if (PsiUtil.getElementType(parent) == ZigTypes.CONTAINER_DECLARATIONS) {
                nestingLevel++;
            }
            parent = parent.getParent();
        }

        if (nestingLevel != 1) {
            return null;
        }

        val actions = ExecutorAction.getActions(0);
        return new Info(getIcon(element), actions, null);
    }

    protected abstract @Nullable PsiElement getDeclaration(@NotNull PsiElement element);

    protected abstract @NotNull Icon getIcon(@NotNull PsiElement element);
}
