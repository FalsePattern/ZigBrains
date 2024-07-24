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

package com.falsepattern.zigbrains.zon.completion;

import com.falsepattern.zigbrains.zon.parser.ZonFile;
import com.falsepattern.zigbrains.zon.psi.ZonEntry;
import com.falsepattern.zigbrains.zon.psi.ZonProperty;
import com.falsepattern.zigbrains.zon.psi.ZonPropertyPlaceholder;
import com.falsepattern.zigbrains.zon.psi.ZonStruct;
import com.falsepattern.zigbrains.zon.psi.ZonTypes;
import com.falsepattern.zigbrains.zon.psi.ZonValuePlaceholder;
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

import static com.falsepattern.zigbrains.common.util.PsiElementUtil.parent;

public class ZonCompletionContributor extends CompletionContributor {
    private static final List<String> ZON_ROOT_KEYS = List.of("name", "version", "minimum_zig_version", "dependencies", "paths");
    private static final List<String> ZON_DEP_KEYS = List.of("url", "hash", "path", "lazy");

    public ZonCompletionContributor() {
        extend(CompletionType.BASIC,
               PlatformPatterns.psiElement()
                               .withParent(PlatformPatterns.psiElement(ZonTypes.PROPERTY_PLACEHOLDER))
                               .withSuperParent(4, PlatformPatterns.psiElement(ZonFile.class)),
               new CompletionProvider<>() {
                   @Override
                   protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
                       var placeholder = parent(parameters.getPosition(), ZonPropertyPlaceholder.class).orElseThrow();
                       var zonEntry = parent(placeholder, ZonEntry.class).orElseThrow();
                       var keys = zonEntry.getKeys();
                       doAddCompletions(placeholder.getText().startsWith("."), keys, ZON_ROOT_KEYS, result);
                   }
               });
        extend(CompletionType.BASIC,
               PlatformPatterns.psiElement()
                               .withParent(PlatformPatterns.psiElement(ZonTypes.VALUE_PLACEHOLDER))
                               .withSuperParent(2, PlatformPatterns.psiElement(ZonTypes.LIST))
                               .withSuperParent(4, PlatformPatterns.psiElement(ZonFile.class)),
               new CompletionProvider<>() {
                   @Override
                   protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
                       var placeholder = parent(parameters.getPosition(), ZonValuePlaceholder.class).orElseThrow();
                       var zonEntry = parent(placeholder, ZonEntry.class).orElseThrow();
                       var keys = zonEntry.getKeys();
                       doAddCompletions(false, Set.of(), ZON_ROOT_KEYS, result);
                   }
               });
        extend(CompletionType.BASIC,
               PlatformPatterns.psiElement()
                               .withParent(PlatformPatterns.psiElement(ZonTypes.PROPERTY_PLACEHOLDER))
                               .withSuperParent(4, PlatformPatterns.psiElement(ZonTypes.PROPERTY))
                               .withSuperParent(7, PlatformPatterns.psiElement(ZonTypes.PROPERTY))
                               .withSuperParent(10, PlatformPatterns.psiElement(ZonFile.class)),
               new CompletionProvider<>() {
                   @Override
                   protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
                       var placeholder = parent(parameters.getPosition(), ZonPropertyPlaceholder.class).orElseThrow();
                       var depEntry = parent(placeholder, ZonEntry.class).orElseThrow();
                       if (!isADependency(depEntry))
                           return;
                       doAddCompletions(placeholder.getText().startsWith("."), depEntry.getKeys(), ZON_DEP_KEYS, result);
                   }
               });
        extend(CompletionType.BASIC,
               PlatformPatterns.psiElement()
                               .withParent(PlatformPatterns.psiElement(ZonTypes.VALUE_PLACEHOLDER))
                               .withSuperParent(2, PlatformPatterns.psiElement(ZonTypes.LIST))
                               .withSuperParent(4, PlatformPatterns.psiElement(ZonTypes.PROPERTY))
                               .withSuperParent(7, PlatformPatterns.psiElement(ZonTypes.PROPERTY))
                               .withSuperParent(10, PlatformPatterns.psiElement(ZonFile.class)),
               new CompletionProvider<>() {
                   @Override
                   protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
                       var placeholder = parent(parameters.getPosition(), ZonValuePlaceholder.class).orElseThrow();
                       var depEntry = parent(placeholder, ZonEntry.class).orElseThrow();
                       if (!isADependency(depEntry))
                           return;
                       doAddCompletions(false, Set.of(), ZON_DEP_KEYS, result);
                   }
               });
        extend(CompletionType.BASIC, PlatformPatterns.psiElement()
                                                     .withParent(PlatformPatterns.psiElement(ZonTypes.VALUE_PLACEHOLDER))
                                                     .withSuperParent(5, PlatformPatterns.psiElement(ZonTypes.PROPERTY))
                                                     .withSuperParent(8, PlatformPatterns.psiElement(ZonTypes.PROPERTY))
                                                     .withSuperParent(11, PlatformPatterns.psiElement(ZonFile.class)),
               new CompletionProvider<>() {
                   @Override
                   protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
                       var placeholder = parent(parameters.getPosition(), ZonValuePlaceholder.class).orElseThrow();
                       var valueProperty = parent(placeholder, ZonProperty.class).orElseThrow();
                       if (!"lazy".equals(valueProperty.getIdentifier().getName())) {
                           return;
                       }
                       if (!isADependency(valueProperty))
                           return;

                       result.addElement(LookupElementBuilder.create("true"));
                       result.addElement(LookupElementBuilder.create("false"));
                   }
               });
    }

    private static boolean isADependency(ZonProperty property) {
        var depEntry = parent(property, ZonEntry.class).orElseThrow();
        return isADependency(depEntry);
    }

    private static boolean isADependency(ZonEntry entry) {
        var parentProperty = parent(entry, ZonProperty.class).flatMap(e -> parent(e, ZonProperty.class)).orElseThrow();
        return "dependencies".equals(parentProperty.getIdentifier().getName());
    }

    private static void doAddCompletions(boolean hasDot, Set<String> current, List<String> expected, CompletionResultSet result) {
        for (var key : expected) {
            if (current.contains(key)) {
                continue;
            }
            result.addElement(LookupElementBuilder.create(hasDot ? key : "." + key));
        }
    }
}
