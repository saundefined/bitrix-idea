package com.github.saundefined.bitrix_idea.php

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker

object BitrixProjectEvents {
    private val EVENT_NAME = Regex("""['"](On[A-Za-z0-9_]+)['"]""")
    private val KEY = Key.create<CachedValue<Set<String>>>("BitrixProjectEvents")

    fun names(project: Project): Set<String> {
        return CachedValuesManager.getManager(project).getCachedValue(project, KEY, {
            CachedValueProvider.Result.create(
                scan(project),
                VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS,
                PsiModificationTracker.MODIFICATION_COUNT,
            )
        }, false)
    }

    private fun scan(project: Project): Set<String> {
        val result = linkedSetOf<String>()
        val scope = GlobalSearchScope.projectScope(project)
        FilenameIndex.getVirtualFilesByName(".settings.php", scope).forEach { file ->
            val text = runCatching { VfsUtilCore.loadText(file) }.getOrNull() ?: return@forEach
            EVENT_NAME.findAll(text).mapTo(result) { it.groupValues[1] }
        }
        return result
    }
}
