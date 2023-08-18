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

package com.falsepattern.zigbrains.zon.pairing;

import com.falsepattern.zigbrains.zon.psi.ZonTypes;
import com.intellij.codeInsight.editorActions.MultiCharQuoteHandler;
import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ZonQuoteHandler extends SimpleTokenSetQuoteHandler implements MultiCharQuoteHandler {
    public ZonQuoteHandler() {
        super(ZonTypes.STRING_LITERAL, ZonTypes.BAD_STRING);
    }

    @Override
    public @Nullable CharSequence getClosingQuote(@NotNull HighlighterIterator iterator, int offset) {
        return "\"";
    }
}
