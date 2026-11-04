package com.github.saundefined.bitrix_idea.roots

import com.github.saundefined.bitrix_idea.settings.BitrixProjectSettings
import com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile

/**
 * Помечает модули в `local/modules` и `bitrix/modules` как source roots и исключает
 * отключённые в настройках модули из индексации. Так классы и namespace остаются
 * доступными, но индекс не раздувается ядром.
 */
object BitrixSourceRootsManager {
    fun configure(project: Project, force: Boolean = false) {
        val settings = BitrixProjectSettings.getInstance(project)
        if (!force && !settings.autoConfigureSourceRoots) return

        val root = BitrixProjectStructure.bitrixRoot(project) ?: return
        val moduleDirectories = BitrixProjectStructure.allModules(project).map { it.directory }
        val extraDirectories = listOfNotNull(
            BitrixProjectStructure.findRelative(project, "local/components"),
            BitrixProjectStructure.findRelative(project, "local/templates"),
            BitrixProjectStructure.findRelative(project, "bitrix/components"),
            BitrixProjectStructure.findRelative(project, "bitrix/templates"),
        )
        val excludedDirectories = listOfNotNull(
            BitrixProjectStructure.findRelative(project, "upload"),
            BitrixProjectStructure.findRelative(project, "bitrix/cache"),
            BitrixProjectStructure.findRelative(project, "bitrix/backup"),
        )
        if (moduleDirectories.isEmpty() && extraDirectories.isEmpty() && excludedDirectories.isEmpty()) return

        val module = findModule(project, root) ?: return
        val disabled = settings.disabledModules.toSet()

        ModuleRootModificationUtil.updateModel(module) { model ->
            val entry = model.contentEntries.firstOrNull { contentEntry ->
                val file = contentEntry.file ?: return@firstOrNull false
                file == root || VfsUtilCore.isAncestor(file, root, false) || VfsUtilCore.isAncestor(root, file, false)
            } ?: model.addContentEntry(root)

            moduleDirectories.forEach { directory ->
                val isDisabled = directory.name in disabled
                val sourceFolder = entry.sourceFolders.firstOrNull { it.file == directory }
                val excludeFolder = entry.excludeFolders.firstOrNull { it.file == directory }

                if (isDisabled) {
                    sourceFolder?.let { entry.removeSourceFolder(it) }
                    if (excludeFolder == null) entry.addExcludeFolder(directory)
                } else {
                    excludeFolder?.let { entry.removeExcludeFolder(it) }
                    if (sourceFolder == null) entry.addSourceFolder(directory, false)
                }
            }

            extraDirectories.forEach { directory ->
                val excludeFolder = entry.excludeFolders.firstOrNull { it.file == directory }
                excludeFolder?.let { entry.removeExcludeFolder(it) }
                if (entry.sourceFolders.none { it.file == directory }) {
                    entry.addSourceFolder(directory, false)
                }
            }

            excludedDirectories.forEach { directory ->
                if (entry.excludeFolders.none { it.file == directory }) {
                    entry.addExcludeFolder(directory)
                }
            }
        }
    }

    private fun findModule(project: Project, root: VirtualFile) = run {
        ProjectFileIndex.getInstance(project).getModuleForFile(root)
            ?: ModuleManager.getInstance(project).modules.firstOrNull { module ->
                ModuleRootManager.getInstance(module).contentRoots.any {
                    it == root || VfsUtilCore.isAncestor(it, root, false)
                }
            }
    }
}
