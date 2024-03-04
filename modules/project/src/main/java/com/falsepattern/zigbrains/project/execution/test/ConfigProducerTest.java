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

package com.falsepattern.zigbrains.project.execution.test;

import com.falsepattern.zigbrains.project.execution.base.ConfigProducerBase;
import com.falsepattern.zigbrains.project.execution.run.ConfigTypeRun;
import com.intellij.execution.actions.ConfigurationFromContext;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public class ConfigProducerTest extends ConfigProducerBase<ZigExecConfigTest> {
    @Override
    public @NotNull ConfigurationFactory getConfigurationFactory() {
        return ConfigTypeTest.getInstance().getConfigurationFactories()[0];
    }

    @Override
    protected boolean setupConfigurationFromContext(@NotNull ZigExecConfigTest configuration, PsiElement element, String filePath, VirtualFile theFile) {
        if (ZigLineMarkerTest.UTILITY_INSTANCE.elementMatches(element)) {
            configuration.filePath = filePath;
            configuration.setName("all tests in " + theFile.getPresentableName());
            return true;
        }
        return false;
    }

    @Override
    protected boolean isConfigurationFromContext(@NotNull ZigExecConfigTest configuration, String filePath, VirtualFile vFile, PsiElement element) {
        return configuration.filePath.equals(filePath);
    }

    @Override
    public boolean shouldReplace(@NotNull ConfigurationFromContext self, @NotNull ConfigurationFromContext other) {
        return self.getConfigurationType() instanceof ConfigTypeRun;
    }
}
