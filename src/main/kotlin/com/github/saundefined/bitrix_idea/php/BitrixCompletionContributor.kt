package com.github.saundefined.bitrix_idea.php

import com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.ArrayHashElement
import com.jetbrains.php.lang.psi.elements.FunctionReference
import com.jetbrains.php.lang.psi.elements.ParameterList
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.intellij.codeInsight.completion.PrefixMatcher
import icons.BitrixIdeaIcons

class BitrixCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), BitrixCompletionProvider())
    }
}

private class BitrixCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val literal = PsiTreeUtil.getParentOfType(
            parameters.position,
            StringLiteralExpression::class.java,
            false,
        ) ?: return
        val project = literal.project
        val model = BitrixProjectStructure.getModel(project)

        val componentId = componentIdForArrayKey(literal)
        if (componentId != null) {
            BitrixComponentParameters.keys(project, componentId)
                ?.sorted()
                ?.forEach { add(result, it, "parameter") }
            return
        }

        BitrixComponentMeta.componentIdForArrayAccess(literal)?.let { ownerId ->
            BitrixComponentMeta.parameters(project, ownerId).values
                .sortedBy { it.key }
                .forEach { parameter ->
                    val tail = listOfNotNull(
                        parameter.name,
                        parameter.defaultValue?.takeIf { it.isNotBlank() }?.let { "по умолчанию: $it" },
                    ).joinToString(" · ")
                    add(result, parameter.key, parameter.type ?: "parameter", tail.ifBlank { null })
                }
            BitrixComponentParameters.standardKeys.sorted().forEach { add(result, it, "standard") }
            return
        }

        if (literal.containingFile.virtualFile?.name == ".settings.php") {
            val modelNamespaces = model.modules.map { "\\${it.namespace}\\Controller" }
            modelNamespaces.forEach { add(result, it, "controller") }
            if (isInsideEvents(literal) || literal.contents.startsWith("On")) {
                (BitrixEventNames.forModule(null) + BitrixProjectEvents.names(project)).sorted()
                    .forEach { add(result, it, "event") }
            }
        }

        val call = BitrixPhp.directCall(literal) ?: return
        val index = BitrixPhp.argumentIndex(literal) ?: return

        when {
            BitrixPhp.isIncludeComponent(call) && index == 0 -> {
                model.components.forEach { add(result, it.id, "component") }
            }

            BitrixPhp.isIncludeComponent(call) && index == 1 -> {
                val component = call.parameterList?.getParameter(0) as? StringLiteralExpression
                val templates = component?.let {
                    BitrixProjectStructure.availableTemplateNames(project, it.contents)
                } ?: setOf(".default")
                templates.sorted().forEach { add(result, it, "template") }
            }

            BitrixPhp.isIncludeModule(call) && index == 0 -> {
                (model.modules.map { it.id } + BitrixCoreModules.enabledIds(project)).distinct().sorted()
                    .forEach { add(result, it, "module") }
            }

            BitrixPhp.isGetMessage(call) && index == 0 -> {
                model.langKeys().sorted().take(MAX_LANG_KEYS).forEach { key ->
                    add(result, key, "lang", BitrixProjectStructure.langValue(project, key))
                }
            }

            BitrixPhp.isEventRegistration(call) && index == 0 -> {
                (BitrixEventNames.modules() + BitrixCoreModules.enabledIds(project)).sorted()
                    .forEach { add(result, it, "module") }
            }

            BitrixPhp.isEventRegistration(call) && index == 1 -> {
                val module = call.parameterList?.getParameter(0) as? StringLiteralExpression
                (BitrixEventNames.forModule(module?.contents) + BitrixProjectEvents.names(project))
                    .sorted()
                    .forEach { add(result, it, "event") }
            }

            BitrixPhp.isAgentCall(call) && index == 0 -> {
                val classNames = runCatching {
                    PhpIndex.getInstance(project).getAllClassFqns(PrefixMatcher.ALWAYS_TRUE)
                }.getOrDefault(emptyList())
                classNames.asSequence()
                    .filter { it.endsWith("Agent") || it.substringAfterLast('\\').endsWith("Agent") }
                    .sorted()
                    .take(MAX_AGENT_CLASSES)
                    .forEach { add(result, "\\$it::run();", "agent") }
            }
        }
    }

    private fun isInsideEvents(element: com.intellij.psi.PsiElement): Boolean {
        var current: com.intellij.psi.PsiElement? = element
        while (current != null) {
            if (current is ArrayHashElement) {
                val key = (current.key as? StringLiteralExpression)?.contents
                if (key == "events" || key == "agents") return true
            }
            current = current.parent
        }
        return false
    }

    private fun componentIdForArrayKey(literal: StringLiteralExpression): String? {
        val hashElement = PsiTreeUtil.getParentOfType(literal, ArrayHashElement::class.java, false) ?: return null
        if (hashElement.key !== literal) return null

        val parameterList = PsiTreeUtil.getParentOfType(hashElement, ParameterList::class.java, false) ?: return null
        val call = PsiTreeUtil.getParentOfType(parameterList, FunctionReference::class.java, true) ?: return null
        if (call.parameterList !== parameterList || !BitrixPhp.isIncludeComponent(call)) return null

        val params = parameterList.parameters.getOrNull(2) ?: return null
        if (!PsiTreeUtil.isAncestor(params, hashElement, false)) return null
        return (parameterList.parameters.getOrNull(0) as? StringLiteralExpression)?.contents
    }

    private fun add(result: CompletionResultSet, value: String, type: String, tailText: String? = null) {
        if (value.isBlank()) return
        var builder = LookupElementBuilder.create(value)
            .withIcon(BitrixIdeaIcons.Bitrix)
            .withTypeText(type)
        if (!tailText.isNullOrBlank()) {
            builder = builder.withTailText(" — $tailText", true)
        }
        result.addElement(builder)
    }

    private companion object {
        const val MAX_LANG_KEYS = 3000
        const val MAX_AGENT_CLASSES = 200
    }
}
