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

package com.falsepattern.zigbrains.zig.pairing;

import com.falsepattern.zigbrains.zig.psi.ZigTypes;
import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ZigBraceMatcher implements PairedBraceMatcher {
    public static final BracePair BRACE_PAIR = new BracePair(ZigTypes.LBRACE, ZigTypes.RBRACE, true);
    public static final BracePair PAREN_PAIR = new BracePair(ZigTypes.LPAREN, ZigTypes.RPAREN, false);
    public static final BracePair BRACKET_PAIR = new BracePair(ZigTypes.LBRACKET, ZigTypes.RBRACKET, false);
    private static final BracePair[] PAIRS = new BracePair[]{BRACE_PAIR, PAREN_PAIR, BRACKET_PAIR};
    @Override
    public BracePair @NotNull [] getPairs() {
        return PAIRS;
    }

    @Override
    public boolean isPairedBracesAllowedBeforeType(@NotNull IElementType lbraceType, @Nullable IElementType contextType) {
        return true;
    }

    @Override
    public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
        var theBrace = file.findElementAt(openingBraceOffset);
        if (theBrace == null) {
            return openingBraceOffset;
        }
        var parent = theBrace.getParent();
        if (parent == null) {
            return openingBraceOffset;
        }
        return parent.getTextOffset();
    }
}
