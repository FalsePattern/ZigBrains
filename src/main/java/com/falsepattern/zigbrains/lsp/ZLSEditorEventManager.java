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

import com.falsepattern.zigbrains.ide.SemaRange;
import com.falsepattern.zigbrains.util.TokenDecoder;
import com.intellij.lang.annotation.Annotation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.event.EditorMouseMotionListener;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.jsonrpc.JsonRpcException;
import org.wso2.lsp4intellij.client.languageserver.ServerOptions;
import org.wso2.lsp4intellij.client.languageserver.requestmanager.RequestManager;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;
import org.wso2.lsp4intellij.editor.EditorEventManager;
import org.wso2.lsp4intellij.listeners.LSPCaretListenerImpl;
import org.wso2.lsp4intellij.requests.Timeouts;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.wso2.lsp4intellij.requests.Timeout.getTimeout;

public class ZLSEditorEventManager extends EditorEventManager {
    public ZLSEditorEventManager(Editor editor,
                                 DocumentListener documentListener,
                                 EditorMouseListener mouseListener,
                                 EditorMouseMotionListener mouseMotionListener,
                                 LSPCaretListenerImpl caretListener,
                                 RequestManager requestmanager,
                                 ServerOptions serverOptions,
                                 LanguageServerWrapper wrapper) {
        super(editor,
              documentListener,
              mouseListener,
              mouseMotionListener,
              caretListener,
              requestmanager,
              serverOptions,
              wrapper);
    }

    @Override
    public synchronized List<Annotation> getAnnotations() {
        return super.getAnnotations();
    }

    public List<SemaRange> semanticHighlighting() {
        var result = new ArrayList<SemaRange>();
        if (!(getRequestManager() instanceof ZLSRequestManager requestManager)) {
            return result;
        }
        var legendOptional = requestManager.sematicLegend();
        if (legendOptional.isEmpty()) {
            return result;
        }
        var legend = legendOptional.get();
        var request = requestManager.semanticTokens(new SemanticTokensParams(getIdentifier()));
        if (request == null) {
            return result;
        }

        try {
            var res = request.get(getTimeout(Timeouts.CODEACTION), TimeUnit.MILLISECONDS);
            wrapper.notifySuccess(Timeouts.CODEACTION);
            if (res == null) {
                return result;
            }
            var responseData = res.getData();
            return TokenDecoder.decodePayload(editor, legend, responseData);
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
