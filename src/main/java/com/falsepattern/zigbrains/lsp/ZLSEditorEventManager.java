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

package com.falsepattern.zigbrains.lsp;

import com.falsepattern.zigbrains.ide.SemaEdit;
import com.falsepattern.zigbrains.util.TokenDecoder;
import com.intellij.lang.annotation.Annotation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.event.EditorMouseMotionListener;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensDelta;
import org.eclipse.lsp4j.SemanticTokensDeltaParams;
import org.eclipse.lsp4j.SemanticTokensEdit;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.jsonrpc.JsonRpcException;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.wso2.lsp4intellij.client.languageserver.ServerOptions;
import org.wso2.lsp4intellij.client.languageserver.requestmanager.RequestManager;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;
import org.wso2.lsp4intellij.editor.EditorEventManager;
import org.wso2.lsp4intellij.listeners.LSPCaretListenerImpl;
import org.wso2.lsp4intellij.requests.Timeouts;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.wso2.lsp4intellij.requests.Timeout.getTimeout;

public class ZLSEditorEventManager extends EditorEventManager {
    private static String previousResultID = null;
    public ZLSEditorEventManager(Editor editor, DocumentListener documentListener, EditorMouseListener mouseListener, EditorMouseMotionListener mouseMotionListener, LSPCaretListenerImpl caretListener, RequestManager requestmanager, ServerOptions serverOptions, LanguageServerWrapper wrapper) {
        super(editor, documentListener, mouseListener, mouseMotionListener, caretListener, requestmanager,
              serverOptions, wrapper);
    }

    @Override
    public synchronized List<Annotation> getAnnotations() {
        return super.getAnnotations();
    }

    public List<SemaEdit> semanticHighlighting() {
        var result = new ArrayList<SemaEdit>();
        if (!(getRequestManager() instanceof ZLSRequestManager requestManager)) {
            return result;
        }
        var legendOptional = requestManager.sematicLegend();
        if (legendOptional.isEmpty()) {
            return result;
        }
        var legend = legendOptional.get();
        CompletableFuture<Either<SemanticTokens, SemanticTokensDelta>> request = null;
        if (previousResultID == null) {
            var param = new SemanticTokensParams(getIdentifier());
            request = requestManager.semanticTokensFull(param)
                                    .thenApply(tokens -> tokens != null ? Either.forLeft(tokens) : null);
        } else {
            var param = new SemanticTokensDeltaParams(getIdentifier(), previousResultID);
            request = requestManager.semanticTokensFullDelta(param);
        }

        try {
            var res = request.get(getTimeout(Timeouts.CODEACTION), TimeUnit.MILLISECONDS);
            wrapper.notifySuccess(Timeouts.CODEACTION);
            if (res == null) {
                return result;
            }
            if (res.isLeft()) {
                var response = res.getLeft();
                previousResultID = response.getResultId();
                var responseData = response.getData();
                result.add(new SemaEdit(0, -1, TokenDecoder.decodePayload(0, editor, legend, responseData)));
            } else {
                var response = res.getRight();
                previousResultID = response.getResultId();
                var edits = response.getEdits();
                for (SemanticTokensEdit edit : edits) {
                    var add = TokenDecoder.decodePayload(0, editor, legend, edit.getData());
                    result.add(new SemaEdit(edit.getStart(), edit.getDeleteCount(), add));
                }
            }
        } catch (TimeoutException | InterruptedException e) {
            LOG.warn(e);
            wrapper.notifyFailure(Timeouts.COMPLETION);
        } catch (JsonRpcException | ExecutionException e) {
            LOG.warn(e);
            wrapper.crashed(e);
        }
        return result;
    }
}
