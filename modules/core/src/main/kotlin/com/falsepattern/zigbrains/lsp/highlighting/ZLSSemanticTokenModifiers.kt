package com.falsepattern.zigbrains.lsp.highlighting

import org.eclipse.lsp4j.SemanticTokenModifiers
import org.jetbrains.annotations.NonNls

@NonNls
object ZLSSemanticTokenModifiers {
    const val Declaration: String = SemanticTokenModifiers.Declaration
    const val Definition: String = SemanticTokenModifiers.Definition
    const val Readonly: String = SemanticTokenModifiers.Readonly
    const val Static: String = SemanticTokenModifiers.Static
    const val Deprecated: String = SemanticTokenModifiers.Deprecated
    const val Abstract: String = SemanticTokenModifiers.Abstract
    const val Async: String = SemanticTokenModifiers.Async
    const val Modification: String = SemanticTokenModifiers.Modification
    const val Documentation: String = SemanticTokenModifiers.Documentation
    const val DefaultLibrary: String = SemanticTokenModifiers.DefaultLibrary

    /** non standard token modifier  */
    const val Generic: String = "generic"
}