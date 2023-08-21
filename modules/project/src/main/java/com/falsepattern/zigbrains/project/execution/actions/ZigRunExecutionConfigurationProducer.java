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

package com.falsepattern.zigbrains.project.execution.actions;

import com.falsepattern.zigbrains.project.execution.configurations.ZigRunExecutionConfiguration;
import com.falsepattern.zigbrains.project.execution.configurations.ZigRunExecutionConfigurationType;
import com.falsepattern.zigbrains.zig.parser.ZigFile;
import com.falsepattern.zigbrains.zig.psi.ZigTypes;
import com.falsepattern.zigbrains.zig.util.PsiUtil;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.ConfigurationFromContext;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ZigRunExecutionConfigurationProducer extends AbstractZigRunExecutionConfigurationProducer{
    @Override
    public @NotNull ConfigurationFactory getConfigurationFactory() {
        return Objects.requireNonNull(
                              ConfigurationTypeUtil.findConfigurationType(ZigRunExecutionConfigurationType.IDENTIFIER))
                      .getConfigurationFactories()[0];
    }

    @Override
    protected boolean setupConfigurationFromContext(@NotNull ZigRunExecutionConfiguration configuration, @NotNull ConfigurationContext context, @NotNull Ref<PsiElement> sourceElement) {
        var loc = context.getLocation();
        if (loc == null) {
            return false;
        }
        var element = loc.getPsiElement();
        var psiFile = element.getContainingFile();
        if (psiFile == null) {
            return false;
        }
        var theFile = psiFile.getVirtualFile();
        var filePath = theFile.getPath();
        if (PsiUtil.getElementType(element) == ZigTypes.KEYWORD_TEST) {
            configuration.command = "test " + filePath;
            configuration.setName("Test " + theFile.getPresentableName());
        } else if ("build.zig".equals(theFile.getName())) {
            configuration.command = "build";
            configuration.setName("Build");
        } else {
            configuration.command = "run " + filePath;
            configuration.setName(theFile.getPresentableName());
        }
        return true;
    }

    @Override
    public boolean isConfigurationFromContext(@NotNull ZigRunExecutionConfiguration configuration, @NotNull ConfigurationContext context) {
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
        val filePath = vFile.getPath();
        if (!configuration.command.contains(filePath)) {
            return configuration.command.startsWith("build") && vFile.getName().equals("build.zig");
        }
        return (PsiUtil.getElementType(element) == ZigTypes.KEYWORD_TEST) == configuration.command.startsWith("test ");
    }

    @Override
    public boolean shouldReplace(@NotNull ConfigurationFromContext self, @NotNull ConfigurationFromContext other) {
        return self.getConfigurationType() instanceof ZigRunExecutionConfigurationType;
    }
}
