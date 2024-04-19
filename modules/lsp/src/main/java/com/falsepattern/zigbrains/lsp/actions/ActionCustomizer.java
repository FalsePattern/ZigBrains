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

package com.falsepattern.zigbrains.lsp.actions;

import com.intellij.codeInsight.actions.ReformatCodeAction;
import com.intellij.codeInsight.actions.ShowReformatFileDialog;
import com.intellij.codeInsight.hint.actions.ShowImplementationsAction;
import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction;
import com.intellij.codeInsight.navigation.actions.GotoImplementationAction;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.impl.DynamicActionConfigurationCustomizer;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ActionCustomizer implements DynamicActionConfigurationCustomizer {
    @RequiredArgsConstructor
    private static class ActionWrapper<T extends AnAction> {
        public final Class<T> klass;
        public final Function<T, WrappedAction<T>> wrapper;
    }
    private static final Map<String, ActionWrapper<?>> actions = new HashMap<>();
    static {
        actions.put("GotoDeclaration", new ActionWrapper<>(GotoDeclarationAction.class, LSPGotoDeclarationAction::new));
        actions.put("GotoImplementation", new ActionWrapper<>(GotoImplementationAction.class, LSPGotoImplementationAction::new));
        actions.put("ReformatCode", new ActionWrapper<>(ReformatCodeAction.class, LSPReformatAction::new));
        actions.put("QuickImplementations", new ActionWrapper<>(ShowImplementationsAction.class,  LSPShowImplementationsAction::new));
        actions.put("ShowReformatFileDialog", new ActionWrapper<>(ShowReformatFileDialog.class, LSPShowReformatDialogAction::new));
    }
    @Override
    public void registerActions(@NotNull ActionManager manager) {
        for (val entry: actions.entrySet()) {
            wrap(manager, entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void unregisterActions(@NotNull ActionManager manager) {
        for (val name: actions.keySet()) {
            unwrap(manager, name);
        }
    }

    private static <T extends AnAction> void wrap(ActionManager manager, String name, ActionWrapper<T> constructor) {
        val oldAction = manager.getAction(name);

        if (constructor.klass.isInstance(oldAction)) {
            val wrapped = constructor.wrapper.apply(constructor.klass.cast(oldAction));
            wrapped.copyFrom(oldAction);
            manager.replaceAction(name, wrapped);
        }
    }

    private static void unwrap(ActionManager manager, String name) {
        val oldAction = manager.getAction(name);
        if (oldAction instanceof WrappedAction<?> w) {
            manager.replaceAction(name, w.wrapped);
        }
    }
}
