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

package com.falsepattern.zigbrains.project.execution.base;

import com.falsepattern.zigbrains.zig.parser.ZigFile;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.LazyRunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public abstract class ConfigProducerBase<T extends ZigExecConfigBase<T>> extends LazyRunConfigurationProducer<T> {
    @NotNull
    @Override
    public abstract ConfigurationFactory getConfigurationFactory();

    @Override
    protected final boolean setupConfigurationFromContext(@NotNull T configuration, @NotNull ConfigurationContext context, @NotNull Ref<PsiElement> sourceElement) {
        var loc = context.getLocation();
        if (loc == null) {
            return false;
        }
        var element = loc.getPsiElement();
        var psiFile = element.getContainingFile();
        if (psiFile == null) {
            return false;
        }
        if (!(psiFile instanceof ZigFile)) {
            return false;
        }
        var theFile = psiFile.getVirtualFile();
        var filePath = theFile.toNioPath();
        return setupConfigurationFromContext(configuration, element, filePath, theFile);
    }

    @Override
    public final boolean isConfigurationFromContext(@NotNull T configuration, @NotNull ConfigurationContext context) {
        if (context.getLocation() == null) {
            return false;
        }
        val element = context.getLocation().getPsiElement();
        val file = element.getContainingFile();
        if (file == null) {
            return false;
        }
        if (!(file instanceof ZigFile)) {
            return false;
        }
        val vFile = file.getVirtualFile();
        val filePath = vFile.toNioPath();
        return isConfigurationFromContext(configuration, filePath, vFile, element);
    }

    /*
    TODO implement these
    @Override
    protected boolean setupConfigurationFromContext(@NotNull ZigExecConfigRun configuration, PsiElement element, String filePath, VirtualFile theFile) {
        if (PsiUtil.getElementType(element) == ZigTypes.KEYWORD_TEST) {
            configuration.command = "test " + filePath;
            configuration.setName("Test " + theFile.getPresentableName());
        } else if ("build.zig".equals(theFile.getName())) {
            configuration.command = "build";
            configuration.setName("Build");
        } else {
            configuration.extraArgs = filePath;
            configuration.setName(theFile.getPresentableName());
        }
        return true;
    }

    @Override
    protected boolean isConfigurationFromContext(@NotNull ZigExecConfigRun configuration, String filePath, VirtualFile vFile, PsiElement element) {
        if (!configuration.command.contains(filePath)) {
            return configuration.command.startsWith("build") && vFile.getName().equals("build.zig");
        }
        return (PsiUtil.getElementType(element) == ZigTypes.KEYWORD_TEST) == configuration.command.startsWith("test ");
    }
     */

    protected abstract boolean setupConfigurationFromContext(@NotNull T configuration, PsiElement element, Path filePath, VirtualFile theFile);
    protected abstract boolean isConfigurationFromContext(@NotNull T configuration, Path filePath, VirtualFile vFile, PsiElement element);
}
