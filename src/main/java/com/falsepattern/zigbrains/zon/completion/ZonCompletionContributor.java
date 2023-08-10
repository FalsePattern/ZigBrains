/*
 * Copyright 2023 FalsePattern
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

package com.falsepattern.zigbrains.zon.completion;

import com.falsepattern.zigbrains.zon.parser.ZonFile;
import com.falsepattern.zigbrains.zon.psi.ZonProperty;
import com.falsepattern.zigbrains.zon.psi.ZonPropertyPlaceholder;
import com.falsepattern.zigbrains.zon.psi.ZonStruct;
import com.falsepattern.zigbrains.zon.psi.ZonTypes;
import com.falsepattern.zigbrains.zon.psi.impl.ZonPsiImplUtil;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class ZonCompletionContributor extends CompletionContributor {
    private static final List<String> ZON_ROOT_KEYS = List.of("name", "version", "dependencies");
    private static final List<String> ZON_DEP_KEYS = List.of("url", "hash");
    public ZonCompletionContributor() {
        extend(CompletionType.BASIC,
               PlatformPatterns.psiElement()
                               .withParent(PlatformPatterns.psiElement(ZonTypes.PROPERTY_PLACEHOLDER))
                               .withSuperParent(3, PlatformPatterns.psiElement(ZonFile.class)),
               new CompletionProvider<>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
                        var placeholder = ZonPsiImplUtil.parent(parameters.getPosition(), ZonPropertyPlaceholder.class);
                        assert placeholder != null;

                        var zonStruct = ZonPsiImplUtil.parent(placeholder, ZonStruct.class);
                        assert zonStruct != null;
                        var keys = ZonPsiImplUtil.getKeys(zonStruct);
                        doAddCompletions(placeholder.getText().startsWith("."), keys, ZON_ROOT_KEYS, result);
                    }
                });
        extend(CompletionType.BASIC,
               PlatformPatterns.psiElement()
                               .withParent(PlatformPatterns.psiElement(ZonTypes.PROPERTY_PLACEHOLDER))
                               .withSuperParent(3, PlatformPatterns.psiElement(ZonTypes.PROPERTY))
                               .withSuperParent(5, PlatformPatterns.psiElement(ZonTypes.PROPERTY))
                               .withSuperParent(7, PlatformPatterns.psiElement(ZonFile.class)),
               new CompletionProvider<>() {
                   @Override
                   protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
                       var placeholder = ZonPsiImplUtil.parent(parameters.getPosition(), ZonPropertyPlaceholder.class);
                       assert placeholder != null;
                       var depStruct = ZonPsiImplUtil.parent(placeholder, ZonStruct.class);
                       assert depStruct != null;
                       var parentProperty = ZonPsiImplUtil.parent(depStruct, ZonProperty.class);
                       assert parentProperty != null;
                       parentProperty = ZonPsiImplUtil.parent(parentProperty, ZonProperty.class);
                       assert parentProperty != null;
                       if (!"dependencies".equals(ZonPsiImplUtil.getText(parentProperty.getIdentifier()))) {
                           return;
                       }
                       doAddCompletions(placeholder.getText().startsWith("."), ZonPsiImplUtil.getKeys(depStruct), ZON_DEP_KEYS, result);
                   }
               });
    }
    private static void doAddCompletions(boolean hasDot, Set<String> current, List<String> expected, CompletionResultSet result) {
        for (var key: expected) {
            if (current.contains(key)) {
                continue;
            }
            result.addElement(LookupElementBuilder.create(hasDot ? key : "." + key));
        }
    }
}
