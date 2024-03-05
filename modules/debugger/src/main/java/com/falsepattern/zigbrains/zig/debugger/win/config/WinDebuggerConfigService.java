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

package com.falsepattern.zigbrains.zig.debugger.win.config;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

@Service(Service.Level.APP)
@State(name = "CPPToolsSettings",
       storages = @Storage("zigbrains.xml"))
public final class WinDebuggerConfigService implements PersistentStateComponent<WinDebuggerConfigService> {
    public String cppToolsPath = "";

    public static WinDebuggerConfigService getInstance() {
        return ApplicationManager.getApplication().getService(WinDebuggerConfigService.class);
    }

    @Override
    public @NotNull WinDebuggerConfigService getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull WinDebuggerConfigService state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
