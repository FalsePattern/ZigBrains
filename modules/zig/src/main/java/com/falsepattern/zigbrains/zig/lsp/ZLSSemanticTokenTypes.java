package com.falsepattern.zigbrains.zig.lsp;

import org.eclipse.lsp4j.SemanticTokenTypes;

public final class ZLSSemanticTokenTypes {
    public static final String Namespace = SemanticTokenTypes.Namespace;
    public static final String Type = SemanticTokenTypes.Type;
    public static final String Class = SemanticTokenTypes.Class;
    public static final String Enum = SemanticTokenTypes.Enum;
    public static final String Interface = SemanticTokenTypes.Interface;
    public static final String Struct = SemanticTokenTypes.Struct;
    public static final String TypeParameter = SemanticTokenTypes.TypeParameter;
    public static final String Parameter = SemanticTokenTypes.Parameter;
    public static final String Variable = SemanticTokenTypes.Variable;
    public static final String Property = SemanticTokenTypes.Property;
    public static final String EnumMember = SemanticTokenTypes.EnumMember;
    public static final String Event = SemanticTokenTypes.Event;
    public static final String Function = SemanticTokenTypes.Function;
    public static final String Method = SemanticTokenTypes.Method;
    public static final String Macro = SemanticTokenTypes.Macro;
    public static final String Keyword = SemanticTokenTypes.Keyword;
    public static final String Modifier = SemanticTokenTypes.Modifier;
    public static final String Comment = SemanticTokenTypes.Comment;
    public static final String String = SemanticTokenTypes.String;
    public static final String Number = SemanticTokenTypes.Number;
    public static final String Regexp = SemanticTokenTypes.Regexp;
    public static final String Operator = SemanticTokenTypes.Operator;
    public static final String Decorator = SemanticTokenTypes.Decorator;
    /** non standard token type */
    public static final String ErrorTag = "errorTag";
    /** non standard token type */
    public static final String Builtin = "builtin";
    /** non standard token type */
    public static final String Label = "label";
    /** non standard token type */
    public static final String KeywordLiteral = "keywordLiteral";

    private ZLSSemanticTokenTypes() {

    }
}
