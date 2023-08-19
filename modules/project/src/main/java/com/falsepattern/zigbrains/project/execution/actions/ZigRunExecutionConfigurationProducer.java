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
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
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
        return true;
    }

    @Override
    public boolean isConfigurationFromContext(@NotNull ZigRunExecutionConfiguration configuration, @NotNull ConfigurationContext context) {
        if (context.getLocation() == null) {
            return false;
        }
        var element = context.getLocation().getPsiElement();
        if (element.getContainingFile() == null) {
            return false;
        }
        return element.getContainingFile() instanceof ZigFile;
    }
}
