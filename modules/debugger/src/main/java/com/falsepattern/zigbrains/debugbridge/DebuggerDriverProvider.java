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

package com.falsepattern.zigbrains.debugbridge;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * getDebuggerConfiguration should return com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration, it's UserDataHolder here to
 * avoid a plugin verifier error.
 */
public interface DebuggerDriverProvider {
    ExtensionPointName<DebuggerDriverProvider> EXTENSION_POINT_NAME = ExtensionPointName.create("com.falsepattern.zigbrains.debuggerDriverProvider");

    static @NotNull Stream<UserDataHolder> findDebuggerConfigurations(Project project) {
        return EXTENSION_POINT_NAME.getExtensionList()
                                   .stream()
                                   .map(it -> it.getDebuggerConfiguration(project))
                                   .filter(Objects::nonNull);
    }

    @Nullable UserDataHolder getDebuggerConfiguration(Project project);
}
