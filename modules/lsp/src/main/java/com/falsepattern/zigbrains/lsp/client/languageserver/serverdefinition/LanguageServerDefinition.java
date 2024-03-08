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
package com.falsepattern.zigbrains.lsp.client.languageserver.serverdefinition;

import com.falsepattern.zigbrains.lsp.common.connection.StreamConnectionProvider;
import com.intellij.openapi.diagnostic.Logger;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.lsp4j.InitializeParams;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A trait representing a ServerDefinition
 */
public abstract class LanguageServerDefinition {

    private static final Logger LOG = Logger.getInstance(LanguageServerDefinition.class);

    public String ext;
    protected Map<String, String> languageIds = Collections.emptyMap();
    private Map<String, StreamConnectionProvider> streamConnectionProviders = new ConcurrentHashMap<>();
    public static final String SPLIT_CHAR = ",";

    /**
     * Starts a Language server for the given directory and returns a tuple (InputStream, OutputStream)
     *
     * @param workingDir The root directory
     * @return The input and output streams of the server
     * @throws IOException if the stream connection provider is crashed
     */
    public Pair<InputStream, OutputStream> start(String workingDir) throws IOException {
        StreamConnectionProvider streamConnectionProvider = streamConnectionProviders.get(workingDir);
        if (streamConnectionProvider != null) {
            return new ImmutablePair<>(streamConnectionProvider.getInputStream(), streamConnectionProvider.getOutputStream());
        } else {
            streamConnectionProvider = createConnectionProvider(workingDir);
            streamConnectionProvider.start();
            streamConnectionProviders.put(workingDir, streamConnectionProvider);
            return new ImmutablePair<>(streamConnectionProvider.getInputStream(), streamConnectionProvider.getOutputStream());
        }
    }

    /**
     * Stops the Language server corresponding to the given working directory
     *
     * @param workingDir The root directory
     */
    public void stop(String workingDir) {
        StreamConnectionProvider streamConnectionProvider = streamConnectionProviders.get(workingDir);
        if (streamConnectionProvider != null) {
            streamConnectionProvider.stop();
            streamConnectionProviders.remove(workingDir);
        } else {
            LOG.warn("No connection for workingDir " + workingDir + " and ext " + ext);
        }
    }

    /**
     * Use this method to modify the {@link InitializeParams} that was initialized by this library. The values
     * assigned to the passed {@link InitializeParams} after this method ends will be the ones sent to the LSP server.
     *
     * @param params the parameters with some prefilled values.
     */
    public void customizeInitializeParams(InitializeParams params) {
    }

    @Override
    public String toString() {
        return "ServerDefinition for " + ext;
    }

    /**
     * Creates a StreamConnectionProvider given the working directory
     *
     * @param workingDir The root directory
     * @return The stream connection provider
     */
    public abstract StreamConnectionProvider createConnectionProvider(String workingDir);

    public ServerListener getServerListener() {
        return ServerListener.DEFAULT;
    }

    /**
     * Return language id for the given extension. if there is no langauge ids registered then the
     * return value will be the value of <code>extension</code>.
     */
    public String languageIdFor(String extension) {
        return languageIds.getOrDefault(extension, extension);
    }
}
