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

package com.falsepattern.zigbrains.zig.completion;

import com.falsepattern.zigbrains.zig.psi.ZigExprList;
import com.intellij.lang.parameterInfo.CreateParameterInfoContext;
import com.intellij.lang.parameterInfo.ParameterInfoHandler;
import com.intellij.lang.parameterInfo.ParameterInfoUIContext;
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import lombok.val;
import org.eclipse.lsp4j.ParameterInformation;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureInformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ZigParameterInfoHandler implements ParameterInfoHandler<PsiElement, ZigParameterInfoHandler.IndexedSignatureInformation> {
    private final WeakHashMap<PsiElement, CompletableFuture<SignatureHelp>> requests = new WeakHashMap<>();
    private final Logger LOG = Logger.getInstance(ZigParameterInfoHandler.class);

    public record IndexedSignatureInformation(SignatureInformation information, boolean active) {}

    private @Nullable PsiElement fetchQuery(@NotNull PsiFile file, int offset) {
        val sourceElem = file.findElementAt(offset);
        if (sourceElem == null)
            return null;
        PsiElement element = PsiTreeUtil.getParentOfType(sourceElem, ZigExprList.class);
        if (element == null) {
            element = sourceElem.getPrevSibling();
            while (element instanceof PsiWhiteSpace)
                element = element.getPrevSibling();
            if (!(element instanceof ZigExprList))
                return null;
        }
//        val editor = FileUtils.editorFromPsiFile(file);
//        if (editor == null)
//            return null;
//        val manager = EditorEventManagerBase.forEditor(editor);
//        if (manager == null)
//            return null;
//        val request = manager.getSignatureHelp(offset);
//        requests.put(element, request);
        return element;
    }

    private @Nullable SignatureHelp getResponse(PsiElement element) {
        val request = requests.get(element);
        if (request == null)
            return null;
        final SignatureHelp response;
        try {
            response = request.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn(e);
            return null;
        }
        return response;
    }

    @Override
    public @Nullable PsiElement findElementForParameterInfo(@NotNull CreateParameterInfoContext context) {
        return fetchQuery(context.getFile(), context.getOffset());
    }

    @Override
    public void showParameterInfo(@NotNull PsiElement element, @NotNull CreateParameterInfoContext context) {
        val response = getResponse(element);
        if (response == null)
            return;
        val signatures = response.getSignatures();
        val indexedSignatures = new IndexedSignatureInformation[signatures.size()];
        val active = response.getActiveSignature();
        for (int i = 0; i < indexedSignatures.length; i++) {
            indexedSignatures[i] = new IndexedSignatureInformation(signatures.get(i), i == active);
        }
        context.setItemsToShow(indexedSignatures);
        context.showHint(element, 0, this);
    }

    @Override
    public @Nullable PsiElement findElementForUpdatingParameterInfo(@NotNull UpdateParameterInfoContext context) {
        return fetchQuery(context.getFile(), context.getOffset());
    }

    @Override
    public void updateParameterInfo(@NotNull PsiElement psiElement, @NotNull UpdateParameterInfoContext context) {
        val response = getResponse(psiElement);
        if (response == null)
            return;
        context.setCurrentParameter(response.getActiveParameter());
    }

    @Override
    public void updateUI(IndexedSignatureInformation p, @NotNull ParameterInfoUIContext context) {
        if (p == null) {
            context.setUIComponentEnabled(false);
            return;
        }

        val txt = new StringBuilder();
        int hStart = 0;
        int hEnd = 0;
        List<ParameterInformation> parameters = p.information.getParameters();
        val active = context.getCurrentParameterIndex();
        for (int i = 0, parametersSize = parameters.size(); i < parametersSize; i++) {
            var param = parameters.get(i);
            if (i != 0) {
                txt.append(", ");
            }
            if (i == active) {
                hStart = txt.length();
            }
            txt.append(param.getLabel().getLeft());
            if (i == active) {
                hEnd = txt.length();
            }
        }

        context.setupUIComponentPresentation(txt.toString(), hStart, hEnd, !p.active, false, true, context.getDefaultParameterColor());
    }
}
