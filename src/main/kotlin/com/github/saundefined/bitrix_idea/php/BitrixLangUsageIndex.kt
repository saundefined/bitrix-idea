package com.github.saundefined.bitrix_idea.php

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

object BitrixLangUsageIndex {
    private val KEY = Key.create<CachedValue<Set<String>>>("BitrixLangUsageIndex")

    fun usedKeys(project: Project): Set<String> {
        return CachedValuesManager.getManager(project).getCachedValue(project, KEY, {
            CachedValueProvider.Result.create(scan(project), PsiModificationTracker.MODIFICATION_COUNT)
        }, false)
    }

    private fun scan(project: Project): Set<String> {
        val result = linkedSetOf<String>()
        FileTypeIndex.getFiles(PhpFileType.INSTANCE, GlobalSearchScope.projectScope(project)).forEach { file ->
            val text = runCatching { VfsUtilCore.loadText(file) }.getOrNull() ?: return@forEach
            if (!text.contains("getMessage", ignoreCase = true)) return@forEach
            val psi = PsiManager.getInstance(project).findFile(file) ?: return@forEach
            PsiTreeUtil.findChildrenOfType(psi, StringLiteralExpression::class.java).forEach { literal ->
                if (BitrixLangSearch.isLangUsage(literal)) result += literal.contents
            }
        }
        return result
    }
}
