package com.github.saundefined.bitrix_idea.php

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

/**
 * Поиск первого использования компонента/шаблона для действий дерева.
 */
object BitrixComponentUsages {
    fun findFirstComponent(project: Project, componentId: String): StringLiteralExpression? {
        return find(project) { literal ->
            val call = BitrixPhp.directCall(literal) ?: return@find false
            BitrixPhp.isIncludeComponent(call) &&
                BitrixPhp.argumentIndex(literal) == 0 &&
                literal.contents == componentId
        }
    }

    fun findFirstTemplate(project: Project, componentId: String, template: String): StringLiteralExpression? {
        return find(project) { literal ->
            val call = BitrixPhp.directCall(literal) ?: return@find false
            if (!BitrixPhp.isIncludeComponent(call) || BitrixPhp.argumentIndex(literal) != 1) return@find false
            if (literal.contents.ifBlank { ".default" } != template) return@find false
            (call.parameterList?.getParameter(0) as? StringLiteralExpression)?.contents == componentId
        }
    }

    private fun find(project: Project, predicate: (StringLiteralExpression) -> Boolean): StringLiteralExpression? {
        val scope = GlobalSearchScope.projectScope(project)
        FileTypeIndex.getFiles(PhpFileType.INSTANCE, scope).forEach { virtualFile ->
            val file = PsiManager.getInstance(project).findFile(virtualFile) ?: return@forEach
            PsiTreeUtil.findChildrenOfType(file, StringLiteralExpression::class.java)
                .firstOrNull(predicate)
                ?.let { return it }
        }
        return null
    }
}
