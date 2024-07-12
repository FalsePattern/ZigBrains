package com.falsepattern.zigbrains.zig.lsp;

import org.eclipse.lsp4j.SemanticTokenModifiers;

public class ZLSSemanticTokenModifiers {
    public static final String Declaration = SemanticTokenModifiers.Declaration;
    public static final String Definition = SemanticTokenModifiers.Definition;
    public static final String Readonly = SemanticTokenModifiers.Readonly;
    public static final String Static = SemanticTokenModifiers.Static;
    public static final String Deprecated = SemanticTokenModifiers.Deprecated;
    public static final String Abstract = SemanticTokenModifiers.Abstract;
    public static final String Async = SemanticTokenModifiers.Async;
    public static final String Modification = SemanticTokenModifiers.Modification;
    public static final String Documentation = SemanticTokenModifiers.Documentation;
    public static final String DefaultLibrary = SemanticTokenModifiers.DefaultLibrary;
    /** non standard token modifier */
    public static final String Generic = "generic";
}
