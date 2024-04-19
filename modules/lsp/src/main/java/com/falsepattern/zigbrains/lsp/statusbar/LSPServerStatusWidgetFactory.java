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
package com.falsepattern.zigbrains.lsp.statusbar;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.widget.StatusBarEditorBasedWidgetFactory;
import lombok.val;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class LSPServerStatusWidgetFactory extends StatusBarEditorBasedWidgetFactory {
     public static final Key<List<LSPServerStatusWidget>> LSP_WIDGETS = Key.create("ZB_LSP_KEYS");
    @Override
    public @NonNls
    @NotNull
    String getId() {
        return "LSP";
    }

    @Override
    public @Nls
    @NotNull
    String getDisplayName() {
        return "Language Server";
    }

    @Override
    public @NotNull StatusBarWidget createWidget(@NotNull Project project) {
        var keys = project.getUserData(LSP_WIDGETS);
        if (keys == null) {
            keys = new ArrayList<>();
            project.putUserData(LSP_WIDGETS, keys);
        }
        val widget = new LSPServerStatusWidget(project);
        keys.add(widget);
        return widget;
    }

    @Override
    public void disposeWidget(@NotNull StatusBarWidget widget) {
        if (widget instanceof LSPServerStatusWidget w) {
            val project = w.project();
            val keys = project.getUserData(LSP_WIDGETS);
            if (keys != null) {
                keys.remove(widget);
            }
        }
        super.disposeWidget(widget);
    }
}
