package com.github.saundefined.bitrix_idea.php

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker

data class BitrixSettingsEntry(
    val name: String,
    val file: VirtualFile,
    val description: String,
)

/**
 * Классы и события из `.settings.php` для Search Everywhere.
 */
object BitrixSettingsEntries {
    private val KEY = Key.create<CachedValue<List<BitrixSettingsEntry>>>("BitrixSettingsEntries")
    private val CLASS = Regex("""(\\?[A-Za-z_][A-Za-z0-9_\\]*)::class""")
    private val EVENT = Regex("""['"](On[A-Za-z0-9_]+)['"]""")

    fun entries(project: Project): List<BitrixSettingsEntry> {
        return CachedValuesManager.getManager(project).getCachedValue(project, KEY, {
            CachedValueProvider.Result.create(
                scan(project),
                VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS,
                PsiModificationTracker.MODIFICATION_COUNT,
            )
        }, false)
    }

    private fun scan(project: Project): List<BitrixSettingsEntry> {
        val result = linkedSetOf<BitrixSettingsEntry>()
        val scope = GlobalSearchScope.projectScope(project)
        FilenameIndex.getVirtualFilesByName(".settings.php", scope).forEach { file ->
            val text = runCatching { VfsUtilCore.loadText(file) }.getOrNull() ?: return@forEach
            CLASS.findAll(text).forEach { match ->
                result += BitrixSettingsEntry(match.groupValues[1].trimStart('\\'), file, "settings class")
            }
            EVENT.findAll(text).forEach { match ->
                result += BitrixSettingsEntry(match.groupValues[1], file, "settings event")
            }
        }
        return result.toList()
    }
}
