// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.falsepattern.zigbrains.backports.com.intellij.lang.documentation

import com.intellij.lang.Language
import com.intellij.lang.documentation.DocumentationMarkup.*
import com.intellij.lang.documentation.DocumentationSettings.InlineCodeHighlightingMode
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.editor.richcopy.HtmlSyntaxInfoUtil
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiElement
import com.falsepattern.zigbrains.backports.com.intellij.ui.components.JBHtmlPaneStyleConfiguration
import com.falsepattern.zigbrains.backports.com.intellij.ui.components.JBHtmlPaneStyleConfiguration.*
import com.intellij.lang.documentation.DocumentationSettings
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.intellij.xml.util.XmlStringUtil
import org.jetbrains.annotations.ApiStatus.Internal

/**
 * This class facilitates generation of highlighted text and code for Quick Documentation.
 * It honors [DocumentationSettings], when highlighting code and links.
 */
object QuickDocHighlightingHelper {

    const val CODE_BLOCK_PREFIX = "<pre><code>"
    const val CODE_BLOCK_SUFFIX = "</code></pre>"

    const val INLINE_CODE_PREFIX = "<code>"
    const val INLINE_CODE_SUFFIX = "</code>"

    /**
     * The returned code block HTML (prefixed with [CODE_BLOCK_PREFIX] and suffixed with [CODE_BLOCK_SUFFIX])
     * has syntax highlighted, if there is language provided and
     * [DocumentationSettings.isHighlightingOfCodeBlocksEnabled] is `true`. The code block will
     * be rendered with a special background if [DocumentationSettings.isCodeBackgroundEnabled] is `true`.
     *
     * Any special HTML characters, like `<` or `>` are escaped.
     */
    @JvmStatic
    @RequiresReadLock
    fun getStyledCodeBlock(project: Project, language: Language?, code: @NlsSafe CharSequence): @NlsSafe String =
        StringBuilder().apply { appendStyledCodeBlock(project, language, code) }.toString()

    /**
     * Appends code block HTML (prefixed with [CODE_BLOCK_PREFIX] and suffixed with [CODE_BLOCK_SUFFIX]),
     * which has syntax highlighted, if there is language provided and
     * [DocumentationSettings.isHighlightingOfCodeBlocksEnabled] is `true`. The code block will
     * be rendered with a special background if [DocumentationSettings.isCodeBackgroundEnabled] is `true`.
     *
     * Any special HTML characters, like `<` or `>` are escaped.
     */
    @JvmStatic
    @RequiresReadLock
    fun StringBuilder.appendStyledCodeBlock(project: Project, language: Language?, code: @NlsSafe CharSequence): @NlsSafe StringBuilder =
        append(CODE_BLOCK_PREFIX)
            .appendHighlightedCode(project, language, DocumentationSettings.isHighlightingOfCodeBlocksEnabled(), code, true)
            .append(CODE_BLOCK_SUFFIX)

    /**
     * The returned inline code HTML (prefixed with [INLINE_CODE_PREFIX] and suffixed with [INLINE_CODE_SUFFIX])
     * has syntax highlighted, if there is language provided and
     * [DocumentationSettings.getInlineCodeHighlightingMode] is [DocumentationSettings.InlineCodeHighlightingMode.SEMANTIC_HIGHLIGHTING].
     * The code block will be rendered with a special background if [DocumentationSettings.isCodeBackgroundEnabled] is `true` and
     * [DocumentationSettings.getInlineCodeHighlightingMode] is not [DocumentationSettings.InlineCodeHighlightingMode.NO_HIGHLIGHTING].
     *
     * Any special HTML characters, like `<` or `>` are escaped.
     */
    @JvmStatic
    @RequiresReadLock
    fun getStyledInlineCode(project: Project, language: Language?, @NlsSafe code: String): @NlsSafe String =
        StringBuilder().apply { appendStyledInlineCode(project, language, code) }.toString()

    /**
     * Appends inline code HTML (prefixed with [INLINE_CODE_PREFIX] and suffixed with [INLINE_CODE_SUFFIX]),
     * which has syntax highlighted, if there is language provided and
     * [DocumentationSettings.getInlineCodeHighlightingMode] is [DocumentationSettings.InlineCodeHighlightingMode.SEMANTIC_HIGHLIGHTING].
     * The code block will be rendered with a special background if [DocumentationSettings.isCodeBackgroundEnabled] is `true` and
     * [DocumentationSettings.getInlineCodeHighlightingMode] is not [DocumentationSettings.InlineCodeHighlightingMode.NO_HIGHLIGHTING].
     *
     * Any special HTML characters, like `<` or `>` are escaped.
     */
    @JvmStatic
    @RequiresReadLock
    fun StringBuilder.appendStyledInlineCode(project: Project, language: Language?, @NlsSafe code: String): StringBuilder =
        append(INLINE_CODE_PREFIX)
            .appendHighlightedCode(
                project, language, DocumentationSettings.getInlineCodeHighlightingMode() == InlineCodeHighlightingMode.SEMANTIC_HIGHLIGHTING, code,
                true)
            .append(INLINE_CODE_SUFFIX)

    /**
     * Tries to guess a registered IDE language based. Useful e.g. for Markdown support
     * to figure out a language to render a code block.
     */
    @JvmStatic
    fun guessLanguage(language: String?): Language? =
        if (language == null)
            null
        else
            Language
                .findInstancesByMimeType(language)
                .asSequence()
                .plus(Language.findInstancesByMimeType("text/$language"))
                .plus(
                    Language.getRegisteredLanguages()
                        .asSequence()
                        .filter { languageMatches(language, it) }
                )
                .firstOrNull()


    private fun StringBuilder.appendHighlightedCode(project: Project, language: Language?, doHighlighting: Boolean,
                                                    code: CharSequence, isForRenderedDoc: Boolean): StringBuilder {
        val processedCode = code.toString().trim('\n', '\r').replace(' ', ' ').trimEnd()
        if (language != null && doHighlighting) {
            HtmlSyntaxInfoUtil.appendHighlightedByLexerAndEncodedAsHtmlCodeSnippet(
                this, project, language, processedCode,
                DocumentationSettings.getHighlightingSaturation(isForRenderedDoc))
        }
        else {
            append(XmlStringUtil.escapeString(processedCode.trimIndent()))
        }
        return this
    }

    private fun languageMatches(langType: String, language: Language): Boolean =
        langType.equals(language.id, ignoreCase = true)
                || FileTypeManager.getInstance().getFileTypeByExtension(langType) === language.associatedFileType

}