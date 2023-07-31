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

import com.falsepattern.zigbrains.HighlightingUtil;
import com.intellij.openapi.diagnostic.Logger;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.FoldingRangeRequestParams;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensDelta;
import org.eclipse.lsp4j.SemanticTokensDeltaParams;
import org.eclipse.lsp4j.SemanticTokensLegend;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.SemanticTokensWithRegistrationOptions;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.wso2.lsp4intellij.client.languageserver.requestmanager.DefaultRequestManager;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ZLSRequestManager extends DefaultRequestManager {
    private static final Logger LOG = Logger.getInstance(ZLSRequestManager.class);

    public ZLSRequestManager(LanguageServerWrapper wrapper, LanguageServer server, LanguageClient client, ServerCapabilities serverCapabilities) {
        super(wrapper, server, client, serverCapabilities);
    }

    public CompletableFuture<SemanticTokens> semanticTokensFull(SemanticTokensParams params) {
        if (checkStatus()) {
            try {
                return (getServerCapabilities().getSemanticTokensProvider() != null)
                       ? getTextDocumentService().semanticTokensFull(params) : null;
            } catch (Exception e) {
                crashed(e);
                return null;
            }
        }
        return null;
    }

    public CompletableFuture<Either<SemanticTokens, SemanticTokensDelta>> semanticTokensFullDelta(SemanticTokensDeltaParams params) {
        if (checkStatus()) {
            try {
                return (getServerCapabilities().getSemanticTokensProvider() != null)
                       ? getTextDocumentService().semanticTokensFullDelta(params) : null;
            } catch (Exception e) {
                crashed(e);
                return null;
            }
        }
        return null;
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        super.didChange(params);
        refreshEditorForFile(params.getTextDocument().getUri());
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        super.didOpen(params);
        refreshEditorForFile(params.getTextDocument().getUri());
    }

    private void refreshEditorForFile(String uri) {
        var editorManager = getWrapper().getEditorManagersFor(uri);
        if (editorManager != null) {
            for (var manager : editorManager) {
                HighlightingUtil.refreshHighlighting(manager);
            }
        }
    }

    public Optional<SemanticTokensLegend> sematicLegend() {
        if (checkStatus()) {
            try {
                return Optional.ofNullable(getServerCapabilities())
                               .map(ServerCapabilities::getSemanticTokensProvider)
                               .map(SemanticTokensWithRegistrationOptions::getLegend);
            } catch (Exception e) {
                crashed(e);
                return Optional.empty();
            }
        }
        return Optional.empty();
    }


    private void crashed(Exception e) {
        LOG.warn(e);
        getWrapper().crashed(e);
    }

    @Override
    public CompletableFuture<List<FoldingRange>> foldingRange(FoldingRangeRequestParams params) {
        var future = super.foldingRange(params);
        return future == null ? null : future.thenApply((range) -> range == null ? Collections.emptyList() : range);
    }
}
