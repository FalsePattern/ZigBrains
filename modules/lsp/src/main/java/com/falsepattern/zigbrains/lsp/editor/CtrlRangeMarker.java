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
package com.falsepattern.zigbrains.lsp.editor;

import com.falsepattern.zigbrains.lsp.utils.DocumentUtils;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import org.eclipse.lsp4j.Location;

import java.awt.Cursor;

public class CtrlRangeMarker {

    Location location;
    private Editor editor;
    private RangeHighlighter range;

    CtrlRangeMarker(Location location, Editor editor, RangeHighlighter range) {
        this.location = location;
        this.editor = editor;
        this.range = range;

    }

    boolean highlightContainsOffset(int offset) {
        if (!isDefinition()) {
            return range.getStartOffset() <= offset && range.getEndOffset() >= offset;
        } else {
            return definitionContainsOffset(offset);
        }
    }

    boolean definitionContainsOffset(int offset) {
        return DocumentUtils.LSPPosToOffset(editor, location.getRange().getStart()) <= offset && offset <= DocumentUtils
                .LSPPosToOffset(editor, location.getRange().getEnd());
    }

    /**
     * Removes the highlighter and restores the default cursor
     */
    void dispose() {
        if (!isDefinition()) {
            editor.getMarkupModel().removeHighlighter(range);
            editor.getContentComponent().setCursor(Cursor.getDefaultCursor());
        }
    }

    /**
     * If the marker points to the definition itself
     */
    boolean isDefinition() {
        return range == null;
    }
}
