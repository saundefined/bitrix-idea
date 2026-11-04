package com.github.saundefined.bitrix_idea.toolwindow

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker

data class BitrixCodeFile(val label: String, val file: VirtualFile)

object BitrixCodeScanner {
    private val KEY = Key.create<CachedValue<Map<String, List<BitrixCodeFile>>>>("BitrixCodeScanner")

    fun scan(project: Project): Map<String, List<BitrixCodeFile>> {
        return CachedValuesManager.getManager(project).getCachedValue(project, KEY, {
            CachedValueProvider.Result.create(
                scanNow(project),
                VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS,
                PsiModificationTracker.MODIFICATION_COUNT,
            )
        }, false)
    }

    private fun scanNow(project: Project): Map<String, List<BitrixCodeFile>> {
        val result = linkedMapOf(
            message("toolwindow.orm") to mutableListOf<BitrixCodeFile>(),
            message("toolwindow.controllers") to mutableListOf(),
            message("toolwindow.handlers") to mutableListOf(),
            message("toolwindow.agents") to mutableListOf(),
            message("toolwindow.console") to mutableListOf(),
            message("toolwindow.services") to mutableListOf(),
        )

        BitrixProjectStructure.getModel(project).modules.forEach { module ->
            val lib = module.directory.findChild("lib") ?: return@forEach
            collect(module.id, lib, lib, result)
        }
        return result.mapValues { it.value.sortedBy { item -> item.label } }
    }

    private fun collect(
        moduleId: String,
        libRoot: VirtualFile,
        directory: VirtualFile,
        result: MutableMap<String, MutableList<BitrixCodeFile>>,
    ) {
        directory.children.forEach { child ->
            if (child.isDirectory) {
                collect(moduleId, libRoot, child, result)
                return@forEach
            }
            if (!child.extension.equals("php", ignoreCase = true)) return@forEach

            val relative = VfsUtilCore.getRelativePath(child, libRoot, '/') ?: child.name
            val label = "$moduleId: $relative"
            val item = BitrixCodeFile(label, child)
            when {
                child.name.endsWith("Table.php") -> result[message("toolwindow.orm")]?.add(item)
                relative.contains("/Controller/") -> result[message("toolwindow.controllers")]?.add(item)
                child.name.endsWith("Agent.php") -> result[message("toolwindow.agents")]?.add(item)
                child.name.endsWith("Handler.php") || child.name.endsWith("EventHandler.php") ->
                    result[message("toolwindow.handlers")]?.add(item)
                relative.contains("/Console/") -> result[message("toolwindow.console")]?.add(item)
                relative.contains("/Service/") -> result[message("toolwindow.services")]?.add(item)
            }
        }
    }
}
