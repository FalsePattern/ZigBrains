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

package com.falsepattern.zigbrains.zon.psi.impl.mixins;

import com.falsepattern.zigbrains.zon.psi.ZonEntry;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class ZonEntryMixinImpl extends ASTWrapperPsiElement implements ZonEntry {
    public ZonEntryMixinImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public Set<String> getKeys() {
        val struct = getStruct();
        if (struct == null)
            return Collections.emptySet();
        var result = new HashSet<String>();
        for (var property : struct.getPropertyList()) {
            result.add(property.getIdentifier().getName());
        }
        return result;
    }
}
