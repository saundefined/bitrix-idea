package com.github.saundefined.bitrix_idea.templates.component

import com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

data class TemplateChainEntry(
    val directory: VirtualFile,
    val kind: String,
    val effective: Boolean,
)

object TemplateChainResolver {
    fun resolve(project: Project, componentId: String, template: String): List<TemplateChainEntry> {
        val dirs = BitrixProjectStructure.findComponentTemplateDirs(project, componentId, template)
        return dirs.mapIndexed { index, directory ->
            TemplateChainEntry(directory, kind(directory), index == 0)
        }
    }

    private fun kind(directory: VirtualFile): String {
        val path = directory.path
        return when {
            path.contains("/local/templates/") -> "Шаблон сайта (local)"
            path.contains("/local/components/") -> "Компонент (local)"
            path.contains("/bitrix/templates/") -> "Шаблон сайта (ядро)"
            path.contains("/bitrix/components/") || path.contains("/install/components/") -> "Компонент (ядро)"
            else -> "Шаблон"
        }
    }
}
