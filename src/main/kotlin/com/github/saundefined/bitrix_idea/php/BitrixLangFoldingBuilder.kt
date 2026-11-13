package com.github.saundefined.bitrix_idea.php

import com.github.saundefined.bitrix_idea.settings.BitrixProjectSettings
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression
import com.jetbrains.php.lang.psi.elements.FunctionReference
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

/**
 * Сворачивает `Loc::getMessage('KEY')` в текст языковой фразы.
 * По умолчанию свёрнуто, поэтому в редакторе видна сразу фраза,
 * а раскрытие возвращает исходный вызов.
 */
class BitrixLangFoldingBuilder : FoldingBuilderEx() {
    override fun buildFoldRegions(
        root: PsiElement,
        document: Document,
        quick: Boolean,
    ): Array<FoldingDescriptor> {
        val result = mutableListOf<FoldingDescriptor>()
        val settings = BitrixProjectSettings.getInstance(root.project)
        if (!settings.langFoldingEnabled) return emptyArray()

        PsiTreeUtil.findChildrenOfType(root, FunctionReference::class.java).forEach { call ->
            if (!BitrixPhp.isGetMessage(call)) return@forEach
            val keyLiteral = call.parameterList?.getParameter(0) as? StringLiteralExpression ?: return@forEach
            val key = keyLiteral.contents
            if (key.isBlank()) return@forEach
            val phrase = BitrixLangMessages.value(root.project, key, settings.langHintLanguage) ?: return@forEach
            if (phrase.isBlank()) return@forEach

            result += FoldingDescriptor(
                call.node,
                call.textRange,
                null,
                "\"${applyParameters(phrase, call)}\"",
            )
        }

        return result.toTypedArray()
    }

    /**
     * Подставляет значения из второго аргумента `Loc::getMessage('KEY', [...])`.
     * Если передан не литеральный массив, фраза показывается без изменений.
     */
    private fun applyParameters(phrase: String, call: FunctionReference): String {
        val values = call.parameterList?.getParameter(1) as? ArrayCreationExpression ?: return phrase
        var result = phrase
        values.hashElements.forEach { element ->
            val placeholder = (element.key as? StringLiteralExpression)?.contents ?: return@forEach
            if (placeholder.isBlank()) return@forEach
            val value = element.value
            val replacement = when (value) {
                is StringLiteralExpression -> value.contents
                null -> return@forEach
                else -> value.text
            }
            result = result.replace(placeholder, replacement)
        }
        return result
    }

    override fun getPlaceholderText(node: ASTNode): String? = null

    override fun isCollapsedByDefault(node: ASTNode): Boolean = true
}
