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

package com.falsepattern.zigbrains.project.util;

import com.falsepattern.zigbrains.project.execution.ZigCapturingProcessHandler;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import lombok.val;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;

public class CLIUtil {
    public static Optional<ProcessOutput> execute(GeneralCommandLine cli, int timeoutMillis) {
        val handler = ZigCapturingProcessHandler.startProcess(cli);
        return handler.map(h -> runProcessWithGlobalProgress(h, timeoutMillis));
    }

    public static ProcessOutput runProcessWithGlobalProgress(CapturingProcessHandler handler, @Nullable Integer timeoutMillis) {
        return runProcess(handler, ProgressManager.getGlobalProgressIndicator(), timeoutMillis);
    }

    public static ProcessOutput runProcess(CapturingProcessHandler handler, @Nullable ProgressIndicator indicator, @Nullable Integer timeoutMillis) {
        if (indicator != null && timeoutMillis != null) {
            return handler.runProcessWithProgressIndicator(indicator, timeoutMillis);
        } else if (indicator != null) {
            return handler.runProcessWithProgressIndicator(indicator);
        } else if (timeoutMillis != null) {
            return handler.runProcess(timeoutMillis);
        } else {
            return handler.runProcess();
        }
    }

    //From Apache Ant
    /**
     * Crack a command line.
     * @param toProcess the command line to process.
     * @return the command line broken into strings.
     * An empty or null toProcess parameter results in a zero sized array.
     */
    public static String[] translateCommandline(String toProcess) throws ConfigurationException {
        if (toProcess == null || toProcess.isEmpty()) {
            //no command? no string
            return new String[0];
        }
        // parse with a simple finite state machine

        final int normal = 0;
        final int inQuote = 1;
        final int inDoubleQuote = 2;
        int state = normal;
        final StringTokenizer tok = new StringTokenizer(toProcess, "\"' ", true);
        final ArrayList<String> result = new ArrayList<>();
        final StringBuilder current = new StringBuilder();
        boolean lastTokenHasBeenQuoted = false;

        while (tok.hasMoreTokens()) {
            String nextTok = tok.nextToken();
            switch (state) {
                case inQuote:
                    if ("'".equals(nextTok)) {
                        lastTokenHasBeenQuoted = true;
                        state = normal;
                    } else {
                        current.append(nextTok);
                    }
                    break;
                case inDoubleQuote:
                    if ("\"".equals(nextTok)) {
                        lastTokenHasBeenQuoted = true;
                        state = normal;
                    } else {
                        current.append(nextTok);
                    }
                    break;
                default:
                    if ("'".equals(nextTok)) {
                        state = inQuote;
                    } else if ("\"".equals(nextTok)) {
                        state = inDoubleQuote;
                    } else if (" ".equals(nextTok)) {
                        if (lastTokenHasBeenQuoted || current.length() > 0) {
                            result.add(current.toString());
                            current.setLength(0);
                        }
                    } else {
                        current.append(nextTok);
                    }
                    lastTokenHasBeenQuoted = false;
                    break;
            }
        }
        if (lastTokenHasBeenQuoted || current.length() > 0) {
            result.add(current.toString());
        }
        if (state == inQuote || state == inDoubleQuote) {
            throw new ConfigurationException("unbalanced quotes in " + toProcess);
        }
        return result.toArray(new String[0]);
    }

    public static List<String> colored(boolean colored, boolean debug) {
        // TODO remove this check once JetBrains implements colored terminal in the debugger
        // https://youtrack.jetbrains.com/issue/CPP-11622/ANSI-color-codes-not-honored-in-Debug-Run-Configuration-output-window
        if (debug) {
            return Collections.emptyList();
        } else {
            return List.of("--color", colored ? "on" : "off");
        }
    }
}
