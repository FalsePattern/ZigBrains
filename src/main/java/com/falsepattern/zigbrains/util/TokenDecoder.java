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

package com.falsepattern.zigbrains.util;

import com.falsepattern.zigbrains.ide.ZigAttributes;
import com.falsepattern.zigbrains.ide.SemaRange;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.util.Computable;
import org.eclipse.lsp4j.SemanticTokensLegend;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TokenDecoder {
    private record Token(int line, int start, int length, int type, int modifiers) {
        public static Token from(Token prevToken, List<Integer> data, int index) {
            int line = data.get(index);
            int start = data.get(index + 1);
            if (prevToken != null) {
                if (line == 0) {
                    start += prevToken.start();
                }
                line += prevToken.line();
            }
            return new Token(line, start, data.get(index + 2), data.get(index + 3), data.get(index + 4));
        }
    }
    public static List<SemaRange> decodePayload(Editor editor, SemanticTokensLegend legend, List<Integer> responseData) {
        var result = new ArrayList<SemaRange>();
        var application = ApplicationManager.getApplication();
        int dataSize = responseData.size();

        Token prevToken = null;
        var types = legend.getTokenTypes();
        var modifiers = legend.getTokenModifiers();
        var modCount = Math.min(31, modifiers.size());
        for (int i = 0; i < dataSize - 5; i += 5) {
            var token = Token.from(prevToken, responseData, i);
            var logiPosStart = new LogicalPosition(token.line(), token.start());
            int tokenStartOffset = application.runReadAction((Computable<Integer>) () -> editor.logicalPositionToOffset(logiPosStart));
            var type = types.size() > token.type() ? types.get(token.type()) : null;
            Set<String> modifierSet = null;
            if (token.modifiers() != 0) {
                modifierSet = new HashSet<>();
                for (int m = 0; m < modCount; m++) {
                    if ((token.modifiers() & (1 << m)) != 0) {
                        modifierSet.add(modifiers.get(m));
                    }
                }
            }
            var key = ZigAttributes.getKey(type, modifierSet);
            key.ifPresent(textAttributesKey -> result.add(
                    new SemaRange(tokenStartOffset, tokenStartOffset + token.length(), textAttributesKey)));
            prevToken = token;
        }

        return result;
    }
}
