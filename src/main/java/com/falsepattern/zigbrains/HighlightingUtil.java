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
import org.wso2.lsp4intellij.editor.EditorEventManager;

import javax.swing.SwingUtilities;

public class HighlightingUtil {
    public static void refreshHighlighting(EditorEventManager eem) {
        var app = ApplicationManager.getApplication();
        if (!(eem instanceof ZLSEditorEventManager manager)) {
            return;
        }
        var editor = manager.editor;
        if (editor == null) {
            return;
        }
        app.runReadAction(() -> {
            var highlightRanges = manager.semanticHighlighting();
            var markup = editor.getMarkupModel();
            SwingUtilities.invokeLater(() -> {
                ApplicationManager.getApplication().runWriteAction(() -> {
                    markup.removeAllHighlighters();
                    for (var range: highlightRanges) {
                        markup.addRangeHighlighter(range.color(), range.start(), range.end(), HighlighterLayer.SYNTAX, HighlighterTargetArea.EXACT_RANGE);
                    }
                });
            });
        });
    }
}
