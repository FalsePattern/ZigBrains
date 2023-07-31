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

package com.falsepattern.zigbrains;

import com.falsepattern.zigbrains.lsp.ZLSEditorEventManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.util.Key;
import org.wso2.lsp4intellij.editor.EditorEventManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;


public class HighlightingUtil {
    private static final Key<Integer> HL_HASH = Key.create("HIGHLIGHTING_HASH");

    public static void refreshHighlighting(EditorEventManager eem) {
        var app = ApplicationManager.getApplication();
        if (!(eem instanceof ZLSEditorEventManager manager)) {
            return;
        }
        var editor = manager.editor;
        if (editor == null) {
            return;
        }
        app.executeOnPooledThread(() -> {
            if (editor.isDisposed()) {
                return;
            }
            var highlightRanges = manager.semanticHighlighting();
            var newHash = highlightRanges.hashCode();
            app.invokeAndWait(() -> {
                if (editor.isDisposed()) {
                    return;
                }
                var markup = editor.getMarkupModel();
                var hash = markup.getUserData(HL_HASH);
                if (hash != null && hash == newHash) {
                    return;
                }
                markup.putUserData(HL_HASH, newHash);
                List<Map.Entry<RangeHighlighter, Integer>> highlightersSorted = null;
                var documentLength = editor.getDocument().getTextLength();
                for (var range : highlightRanges) {
                    if (range.start() == 0 && range.remove() == -1 && highlightRanges.size() == 1) {
                        markup.removeAllHighlighters();
                    } else if (highlightersSorted == null) {
                        highlightersSorted = new ArrayList<>(Stream.of(markup.getAllHighlighters())
                                                                   .map(hl -> Map.entry(hl, hl.getStartOffset()))
                                                                   .sorted(Comparator.comparingInt(Map.Entry::getValue))
                                                                   .toList());
                    }
                    var start = range.start();
                    var toRemove = range.remove();
                    if (toRemove > 0) {
                        for (int i = 0; i < highlightersSorted.size(); i++) {
                            var hl = highlightersSorted.get(i);
                            if (hl.getValue() >= start) {
                                for (int j = 0; j < toRemove; j++) {
                                    highlightersSorted.remove(i);
                                    markup.removeHighlighter(hl.getKey());
                                }
                            }
                        }
                    }
                    for (var edit : range.add()) {
                        var end = edit.end();
                        if (end > documentLength - 1) {
                            end = documentLength - 1;
                        }
                        markup.addRangeHighlighter(edit.color(), edit.start(), end, HighlighterLayer.SYNTAX,
                                                   HighlighterTargetArea.EXACT_RANGE);
                    }
                }
            });
        });
    }
}
