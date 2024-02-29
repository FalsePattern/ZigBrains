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

package com.falsepattern.zigbrains.zig.environment;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public interface ZLSConfigProvider {
    ExtensionPointName<ZLSConfigProvider> EXTENSION_POINT_NAME = ExtensionPointName.create("com.falsepattern.zigbrains.zlsConfigProvider");

    static @NotNull ZLSConfig findEnvironment(Project project) {
        return EXTENSION_POINT_NAME.getExtensionList()
                                   .stream()
                                   .map(it -> it.getEnvironment(project))
                                   .reduce(ZLSConfig.EMPTY, ZLSConfig::overrideWith);
    }

    @NotNull ZLSConfig getEnvironment(Project project);
}
