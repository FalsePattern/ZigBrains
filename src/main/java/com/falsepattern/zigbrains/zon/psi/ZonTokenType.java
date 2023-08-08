package com.falsepattern.zigbrains.zon.psi;

import com.falsepattern.zigbrains.zon.ZonLanguage;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class ZonTokenType extends IElementType {
    public ZonTokenType(@NonNls @NotNull String debugName) {
        super(debugName, ZonLanguage.INSTANCE);
    }
}
