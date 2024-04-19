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

import com.falsepattern.zigbrains.lsp.IntellijLanguageClient;
import com.falsepattern.zigbrains.lsp.editor.EditorEventManager;
import com.falsepattern.zigbrains.lsp.editor.EditorEventManagerBase;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.OverridingAction;
import com.intellij.openapi.actionSystem.ShortcutSet;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import lombok.val;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Supplier;

public abstract class WrappedAction<T extends AnAction> extends AnAction implements OverridingAction {
    public final T wrapped;

//    private static class Reflector {
//        static Field templatePresentation;
//        static Field myShortcutSet;
//        static Field myEnabledInModalContext;
//        static Field myIsDefaultIcon;
//        static Field myWorksInInjected;
//        static Field myActionTextOverrides;
//        static Field mySynonyms;
//        static Field[] fields;
//
//        static {
//            val theClass = AnAction.class;
//            val validFields = new ArrayList<Field>();
//            try {
//                templatePresentation = theClass.getDeclaredField("templatePresentation");
//                templatePresentation.setAccessible(true);
//                validFields.add(templatePresentation);
//            } catch (NoSuchFieldException e) {
//                e.printStackTrace();
//            }
//            try {
//                myShortcutSet = theClass.getDeclaredField("myShortcutSet");
//                myShortcutSet.setAccessible(true);
//                validFields.add(myShortcutSet);
//            } catch (NoSuchFieldException e) {
//                e.printStackTrace();
//            }
//            try {
//                myEnabledInModalContext = theClass.getDeclaredField("myEnabledInModalContext");
//                myEnabledInModalContext.setAccessible(true);
//                validFields.add(myEnabledInModalContext);
//            } catch (NoSuchFieldException e) {
//                e.printStackTrace();
//            }
//            try {
//                myIsDefaultIcon = theClass.getDeclaredField("myIsDefaultIcon");
//                myIsDefaultIcon.setAccessible(true);
//                validFields.add(myIsDefaultIcon);
//            } catch (NoSuchFieldException e) {
//                e.printStackTrace();
//            }
//            try {
//                myWorksInInjected = theClass.getDeclaredField("myWorksInInjected");
//                myWorksInInjected.setAccessible(true);
//                validFields.add(myWorksInInjected);
//            } catch (NoSuchFieldException e) {
//                e.printStackTrace();
//            }
//            try {
//                myActionTextOverrides = theClass.getDeclaredField("myActionTextOverrides");
//                myActionTextOverrides.setAccessible(true);
//                validFields.add(myActionTextOverrides);
//            } catch (NoSuchFieldException e) {
//                e.printStackTrace();
//            }
//            try {
//                mySynonyms = theClass.getDeclaredField("mySynonyms");
//                mySynonyms.setAccessible(true);
//                validFields.add(mySynonyms);
//            } catch (NoSuchFieldException e) {
//                e.printStackTrace();
//            }
//            fields = validFields.toArray(new Field[0]);
//        }
//    }

    public WrappedAction(T wrapped) {
        this.wrapped = wrapped;
//        if (Reflector.fields != null) {
//            for (val field: Reflector.fields) {
//                assimilate(field);
//            }
//        }
    }
    private void assimilate(Field field) {
        if (field == null)
            return;
        try {
            field.set(this, field.get(wrapped));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isDumbAware() {
        return wrapped.isDumbAware();
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return wrapped.getActionUpdateThread();
    }

    @Override
    public boolean displayTextInToolbar() {
        return super.displayTextInToolbar();
    }

    @Override
    public boolean useSmallerFontForTextInToolbar() {
        return super.useSmallerFontForTextInToolbar();
    }

    @Override
    public void setShortcutSet(@NotNull ShortcutSet shortcutSet) {
        super.setShortcutSet(shortcutSet);
    }

    @Override
    public void setDefaultIcon(boolean isDefaultIconSet) {
        wrapped.setDefaultIcon(isDefaultIconSet);
    }

    @Override
    public boolean isDefaultIcon() {
        return wrapped.isDefaultIcon();
    }

    @Override
    public void setInjectedContext(boolean worksInInjected) {
        wrapped.setInjectedContext(worksInInjected);
    }

    @Override
    public boolean isInInjectedContext() {
        return wrapped.isInInjectedContext();
    }

    @Override
    public void addSynonym(@NotNull Supplier<@Nls String> text) {
        wrapped.addSynonym(text);
    }

    @Override
    public @NotNull List<Supplier<String>> getSynonyms() {
        return wrapped.getSynonyms();
    }

    @Override
    public final void update(@NotNull AnActionEvent e) {
        val manager = tryGetEventManager(e);
        if (manager == null) {
            wrapped.update(e);
            return;
        }
        val file = tryGetPSIFileLSPAware(manager);
        if (file == null) {
            wrapped.update(e);
            return;
        }
        updateLSP(e, manager, file);
    }

    @Override
    public final void beforeActionPerformedUpdate(@NotNull AnActionEvent e) {
        val manager = tryGetEventManager(e);
        if (manager == null) {
            wrapped.beforeActionPerformedUpdate(e);
            return;
        }
        val file = tryGetPSIFileLSPAware(manager);
        if (file == null) {
            wrapped.beforeActionPerformedUpdate(e);
            return;
        }
        beforeActionPerformedUpdateLSP(e, manager, file);
    }

    @Override
    public final void actionPerformed(@NotNull AnActionEvent e) {
        val manager = tryGetEventManager(e);
        if (manager == null) {
            wrapped.actionPerformed(e);
            return;
        }
        val file = tryGetPSIFileLSPAware(manager);
        if (file == null) {
            wrapped.actionPerformed(e);
            return;
        }
        actionPerformedLSP(e, manager, file);
    }

    private static EditorEventManager tryGetEventManager(AnActionEvent e) {
        val editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null)
            return null;
        return EditorEventManagerBase.forEditor(editor);
    }

    private static PsiFile tryGetPSIFileLSPAware(EditorEventManager manager) {
        val project = manager.getProject();
        val editor = manager.editor;
        if (project == null)
            return null;
        val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (file == null || !IntellijLanguageClient.isExtensionSupported(file.getVirtualFile()))
            return null;
        return file;
    }

    protected abstract void actionPerformedLSP(AnActionEvent e, EditorEventManager manager, PsiFile file);

    protected void beforeActionPerformedUpdateLSP(AnActionEvent e, EditorEventManager manager, PsiFile file) {

    }

    protected void updateLSP(AnActionEvent e, EditorEventManager manager, PsiFile file) {

    }
}
