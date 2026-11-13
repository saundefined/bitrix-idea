package com.github.saundefined.bitrix_idea.settings

import com.github.saundefined.bitrix_idea.library.BitrixLibraryRootsProvider
import com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
import com.intellij.openapi.project.Project

data class BitrixDiagnosticsSnapshot(
    val coreSource: String,
    val corePath: String,
    val version: String,
    val modulesTotal: Int,
    val modulesIndexed: Int,
    val components: Int,
    val templates: Int,
    val langFiles: Int,
    val excludedModules: List<String>,
    val excludedComponents: List<String>,
    val excludedTemplates: List<String>,
)

/**
 * Сводка по проекту для страницы настроек.
 */
object BitrixDiagnostics {
    fun collect(project: Project): BitrixDiagnosticsSnapshot {
        val settings = BitrixProjectSettings.getInstance(project)
        val model = BitrixProjectStructure.getModel(project)
        val bundled = BitrixLibraryRootsProvider.bundledModules()
        val projectModules = BitrixProjectStructure.allModules(project)

        val corePath = when (settings.coreSource) {
            BitrixCoreSource.BUNDLED -> bundled.firstOrNull()?.directory?.parent?.path.orEmpty()
            BitrixCoreSource.PROJECT -> BitrixProjectStructure.findRelative(project, "bitrix/modules")?.path.orEmpty()
        }
        val totalModules = (projectModules + bundled).distinctBy { it.id }.size
        val indexedModules = (projectModules + bundled)
            .distinctBy { it.id }
            .count { it.id !in settings.disabledModules }

        return BitrixDiagnosticsSnapshot(
            coreSource = settings.coreSource.displayName,
            corePath = corePath.ifBlank { "—" },
            version = BitrixProjectStructure.bitrixVersion(project) ?: "—",
            modulesTotal = totalModules,
            modulesIndexed = indexedModules,
            components = model.components.size,
            templates = model.siteTemplates.size,
            langFiles = model.langFiles.size,
            excludedModules = settings.disabledModules.sorted(),
            excludedComponents = settings.excludedComponentIds.sorted(),
            excludedTemplates = settings.excludedSiteTemplates.sorted(),
        )
    }
}
