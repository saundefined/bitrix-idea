package com.github.saundefined.bitrix_idea.php

import com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.psi.elements.ArrayAccessExpression
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

object BitrixLangSearch {
    fun isLangDefinition(literal: StringLiteralExpression): Boolean {
        val arrayAccess = PsiTreeUtil.getParentOfType(literal, ArrayAccessExpression::class.java, false)
            ?: return false
        if (arrayAccess.index?.value !== literal) return false
        return arrayAccess.value?.text.orEmpty().contains("\$MESS")
    }

    fun isLangUsage(literal: StringLiteralExpression): Boolean {
        val call = BitrixPhp.directCall(literal) ?: return false
        return BitrixPhp.isGetMessage(call) && BitrixPhp.argumentIndex(literal) == 0
    }

    fun findDefinitions(project: Project, key: String): List<StringLiteralExpression> {
        val indexed = BitrixLangDefinitionIndex.findFiles(project, key)
        if (indexed.isNotEmpty()) {
            return indexed.mapNotNull { virtualFile ->
                val file = PsiManager.getInstance(project).findFile(virtualFile) ?: return@mapNotNull null
                PsiTreeUtil.findChildrenOfType(file, StringLiteralExpression::class.java)
                    .firstOrNull { it.contents == key && isLangDefinition(it) }
            }
        }
        return BitrixProjectStructure.findLangEntries(project, key)
            .mapNotNull { entry ->
                BitrixProjectStructure.psiElementAt(project, entry)
                    ?.let { PsiTreeUtil.getParentOfType(it, StringLiteralExpression::class.java, false) }
            }
    }

    fun findUsages(project: Project, key: String): List<StringLiteralExpression> {
        val result = mutableListOf<StringLiteralExpression>()
        val scope = GlobalSearchScope.projectScope(project)
        FileTypeIndex.getFiles(PhpFileType.INSTANCE, scope).forEach { virtualFile ->
            val file = PsiManager.getInstance(project).findFile(virtualFile) ?: return@forEach
            PsiTreeUtil.findChildrenOfType(file, StringLiteralExpression::class.java).forEach { literal ->
                if (literal.contents != key) return@forEach
                if (!isLangUsage(literal)) return@forEach
                result += literal
            }
        }
        return result
    }
}
