package com.github.saundefined.bitrix_idea.php

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
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

        PsiTreeUtil.findChildrenOfType(root, FunctionReference::class.java).forEach { call ->
            if (!BitrixPhp.isGetMessage(call)) return@forEach
            val keyLiteral = call.parameterList?.getParameter(0) as? StringLiteralExpression ?: return@forEach
            val key = keyLiteral.contents
            if (key.isBlank()) return@forEach
            val phrase = BitrixLangMessages.value(root.project, key) ?: return@forEach
            if (phrase.isBlank()) return@forEach

            result += FoldingDescriptor(
                call.node,
                call.textRange,
                null,
                "\"$phrase\"",
            )
        }

        return result.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String? = null

    override fun isCollapsedByDefault(node: ASTNode): Boolean = true
}
