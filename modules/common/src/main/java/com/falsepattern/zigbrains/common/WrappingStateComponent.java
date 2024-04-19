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

package com.falsepattern.zigbrains.common;

import com.intellij.openapi.components.PersistentStateComponent;
import org.jetbrains.annotations.NotNull;

public abstract class WrappingStateComponent<T> implements PersistentStateComponent<T> {
    private T state;
    public WrappingStateComponent(@NotNull T initialState) {
        this.state = initialState;
    }

    @Override
    public @NotNull T getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull T state) {
        this.state = state;
    }
}
