package com.github.saundefined.bitrix_idea.php

import com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.AssignmentExpression
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

object BitrixLangMessages {
    fun value(project: Project, key: String): String? {
        val entry = BitrixProjectStructure.findLangEntries(project, key).firstOrNull() ?: return null
        entry.value?.takeIf { it.isNotBlank() }?.let { return it }
        val file = PsiManager.getInstance(project).findFile(entry.file) ?: return null
        val keyLiteral = file.findElementAt(entry.offset)
            ?.let { PsiTreeUtil.getParentOfType(it, StringLiteralExpression::class.java, false) }
            ?: return null
        val assignment = PsiTreeUtil.getParentOfType(keyLiteral, AssignmentExpression::class.java, false)
            ?: return null
        val value = assignment.value ?: return null
        return when (value) {
            is StringLiteralExpression -> value.contents
            else -> value.text
        }
    }
}
