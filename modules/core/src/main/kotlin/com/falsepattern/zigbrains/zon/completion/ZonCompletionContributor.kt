package com.falsepattern.zigbrains.zon.completion

import com.falsepattern.zigbrains.zon.psi.ZonEntry
import com.falsepattern.zigbrains.zon.psi.ZonFile
import com.falsepattern.zigbrains.zon.psi.ZonProperty
import com.falsepattern.zigbrains.zon.psi.ZonPropertyPlaceholder
import com.falsepattern.zigbrains.zon.psi.ZonTypes
import com.falsepattern.zigbrains.zon.psi.ZonValuePlaceholder
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext

class ZonCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            psiElement()
                .withParent(psiElement(ZonTypes.PROPERTY_PLACEHOLDER))
                .withSuperParent(4, psiOfType<ZonFile>())
        ) { parameters, _, result ->
            val placeholder = parameters.position.parentOfType<ZonPropertyPlaceholder>() ?: return@extend
            val zonEntry = placeholder.parentOfType<ZonEntry>() ?: return@extend
            val keys = zonEntry.keys
            doAddCompletions(placeholder.text.startsWith('.'), keys, ZON_ROOT_KEYS, result)
        }
        extend(
            CompletionType.BASIC,
            psiElement()
                .withParent(psiElement(ZonTypes.VALUE_PLACEHOLDER))
                .withSuperParent(2, psiElement(ZonTypes.LIST))
                .withSuperParent(4, psiOfType<ZonFile>())
        ) { _, _, result ->
            doAddCompletions(false, emptySet(), ZON_ROOT_KEYS, result)
        }
        extend(
            CompletionType.BASIC,
            psiElement()
                .withParent(psiElement(ZonTypes.PROPERTY_PLACEHOLDER))
                .withSuperParent(4, psiElement(ZonTypes.PROPERTY))
                .withSuperParent(7, psiElement(ZonTypes.PROPERTY))
                .withSuperParent(10, psiOfType<ZonFile>())
        ) { parameters, _, result ->
            val placeholder = parameters.position.parentOfType<ZonPropertyPlaceholder>() ?: return@extend
            val depEntry = placeholder.parentOfType<ZonEntry>() ?: return@extend
            if (depEntry.isDependency) {
                doAddCompletions(placeholder.text.startsWith('.'), emptySet(), ZON_DEP_KEYS, result)
            }
        }
        extend(
            CompletionType.BASIC,
            psiElement()
                .withParent(psiElement(ZonTypes.VALUE_PLACEHOLDER))
                .withSuperParent(2, psiElement(ZonTypes.LIST))
                .withSuperParent(4, psiElement(ZonTypes.PROPERTY))
                .withSuperParent(7, psiElement(ZonTypes.PROPERTY))
                .withSuperParent(10, psiOfType<ZonFile>())
        ) {parameters, _, result ->
            val placeholder = parameters.position.parentOfType<ZonValuePlaceholder>() ?: return@extend
            val depEntry = placeholder.parentOfType<ZonEntry>() ?: return@extend
            if (depEntry.isDependency) {
                doAddCompletions(false, emptySet(), ZON_DEP_KEYS, result)
            }
        }
        extend(
            CompletionType.BASIC,
            psiElement()
                .withParent(psiElement(ZonTypes.VALUE_PLACEHOLDER))
                .withSuperParent(5, psiElement(ZonTypes.PROPERTY))
                .withSuperParent(8, psiElement(ZonTypes.PROPERTY))
                .withSuperParent(11, psiOfType<ZonFile>())
        ) { parameters, _, result ->
            val placeholder = parameters.position.parentOfType<ZonValuePlaceholder>() ?: return@extend
            val valueProperty = placeholder.parentOfType<ZonProperty>() ?: return@extend
            if (valueProperty.isDependency && valueProperty.identifier.value == "lazy") {
                result.addElement(LookupElementBuilder.create("true"))
                result.addElement(LookupElementBuilder.create("false"))
            }
        }
    }

    private inline fun extend(type: CompletionType, place: ElementPattern<PsiElement>, crossinline action: Processor) {
        extend(type, place, object : CompletionProvider<CompletionParameters>() {
            override fun addCompletions(
                parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet
            ) = action(parameters, context, result)
        })
    }
}

private typealias Processor = (
    parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet
) -> Unit

private val ZON_ROOT_KEYS: List<String> =
    listOf("name", "version", "minimum_zig_version", "dependencies", "paths")
private val ZON_DEP_KEYS: List<String> = listOf("url", "hash", "path", "lazy")

private fun doAddCompletions(
    hasDot: Boolean, current: Set<String>, expected: List<String>, result: CompletionResultSet
) {
    for (key in expected) {
        if (current.contains(key)) {
            continue
        }
        result.addElement(LookupElementBuilder.create(if (hasDot) key else ".$key"))
    }
}

private val ZonProperty.isDependency: Boolean
    get() {
        return parentOfType<ZonEntry>()?.isDependency ?: false
    }

private val ZonEntry.isDependency: Boolean
    get() {
        val parentProperty = parentOfType<ZonProperty>()?.parentOfType<ZonProperty>() ?: return false
        return parentProperty.identifier.value == "dependencies"
    }

private inline fun <reified T : PsiElement> psiOfType(): PsiElementPattern.Capture<T> =
    psiElement(T::class.java)