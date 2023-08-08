package com.falsepattern.zigbrains.zon.parser;

import com.falsepattern.zigbrains.zon.ZonFileType;
import com.falsepattern.zigbrains.zon.ZonLanguage;
import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;

public class ZonFile extends PsiFileBase {
    public ZonFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, ZonLanguage.INSTANCE);
    }

    @Override
    public @NotNull FileType getFileType() {
        return ZonFileType.INSTANCE;
    }

    @Override
    public String toString() {
        return "Zon File";
    }
}
