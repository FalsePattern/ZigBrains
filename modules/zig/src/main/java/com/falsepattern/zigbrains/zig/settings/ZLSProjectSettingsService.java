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

package com.falsepattern.zigbrains.zig.settings;

import com.falsepattern.zigbrains.common.WrappingStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import lombok.val;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

@Service(Service.Level.PROJECT)
@State(name = "ZLSSettings",
       storages = @Storage("zigbrains.xml"))
public final class ZLSProjectSettingsService extends WrappingStateComponent<ZLSSettings> {
    public ZLSProjectSettingsService() {
        super(new ZLSSettings());
    }

    public static ZLSProjectSettingsService getInstance(Project project) {
        return project.getService(ZLSProjectSettingsService.class);
    }

    public boolean isModified(ZLSSettings otherData) {
        val myData = this.getState();
        boolean modified = zlsSettingsModified(otherData);
        modified |= myData.asyncFolding != otherData.asyncFolding;
        return modified;
    }

    public boolean zlsSettingsModified(ZLSSettings otherData) {
        val myData = this.getState();
        boolean modified = !Objects.equals(myData.zlsPath, otherData.zlsPath);
        modified |= !Objects.equals(myData.zlsConfigPath, otherData.zlsConfigPath);
        modified |= myData.debug != otherData.debug;
        modified |= myData.messageTrace != otherData.messageTrace;
        modified |= myData.increaseTimeouts != otherData.increaseTimeouts;
        modified |= myData.buildOnSave != otherData.buildOnSave;
        modified |= !Objects.equals(myData.buildOnSaveStep, otherData.buildOnSaveStep);
        modified |= myData.highlightGlobalVarDeclarations != otherData.highlightGlobalVarDeclarations;
        modified |= myData.dangerousComptimeExperimentsDoNotEnable != otherData.dangerousComptimeExperimentsDoNotEnable;
        return modified;
    }
}
