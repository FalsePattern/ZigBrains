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

package com.falsepattern.zigbrains.project.execution.build;

import com.falsepattern.zigbrains.project.execution.base.ConfigProducerBase;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class ConfigProducerBuild extends ConfigProducerBase<ZigExecConfigBuild> {
    @Override
    public @NotNull ConfigurationFactory getConfigurationFactory() {
        return ConfigTypeBuild.getInstance().getConfigurationFactories()[0];
    }

    @Override
    protected boolean setupConfigurationFromContext(@NotNull ZigExecConfigBuild configuration, PsiElement element, Path filePath, VirtualFile theFile) {
        if (ZigLineMarkerBuild.UTILITY_INSTANCE.elementMatches(element)) {
            configuration.setName("Build and Run");
            configuration.getBuildSteps().args = new String[]{"run"};
            return true;
        }
        return false;
    }

    @Override
    protected boolean isConfigurationFromContext(@NotNull ZigExecConfigBuild configuration, Path filePath, VirtualFile vFile, PsiElement element) {
        val p = configuration.getWorkingDirectory().getPath();
        if (p.isEmpty())
            return false;
        val path = p.get();
        return filePath.getParent().equals(path);
    }
}
