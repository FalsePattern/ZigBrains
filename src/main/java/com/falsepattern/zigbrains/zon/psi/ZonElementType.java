package com.falsepattern.zigbrains.zon.psi;

import com.falsepattern.zigbrains.zon.ZonLanguage;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class ZonElementType extends IElementType {
    public ZonElementType(@NonNls @NotNull String debugName) {
        super(debugName, ZonLanguage.INSTANCE);
    }
}
