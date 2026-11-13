package com.github.saundefined.bitrix_idea.toolwindow

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile

data class BitrixHealthIssue(
    val message: String,
    val file: VirtualFile?,
)

/**
 * Быстрые проверки проекта для окна диагностики.
 */
object BitrixHealthChecker {
    fun collect(project: Project): Map<String, List<BitrixHealthIssue>> {
        val model = BitrixProjectStructure.getModel(project)
        val result = linkedMapOf<String, MutableList<BitrixHealthIssue>>()

        fun add(category: String, issue: BitrixHealthIssue) {
            result.getOrPut(category) { mutableListOf() } += issue
        }

        model.components.forEach { component ->
            if (component.entryFiles().isEmpty()) {
                add(message("health.category.structure"), BitrixHealthIssue(
                    message("health.issue.component.entry", component.id), component.directory))
            }
            if (component.directory.findChild(".description.php") == null) {
                add(message("health.category.structure"), BitrixHealthIssue(
                    message("health.issue.component.description", component.id),
                    component.directory.findChild("class.php") ?: component.directory))
            }
            val templates = component.directory.findChild("templates")
            if (templates == null || templates.children.none { it.isDirectory }) {
                add(message("health.category.templates"), BitrixHealthIssue(
                    message("health.issue.component.templates", component.id), component.directory))
            } else {
                templates.children.filter { it.isDirectory }.forEach { template ->
                    if (template.findChild("template.php") == null) {
                        add(message("health.category.templates"), BitrixHealthIssue(
                            message("health.issue.template.missing", "${component.id}:${template.name}"), template))
                    }
                }
            }
            if (component.directory.findChild("lang") == null) {
                add(message("health.category.lang"), BitrixHealthIssue(
                    message("health.issue.component.lang", component.id), component.directory))
            }
        }

        model.langEntries.forEach { (key, entries) ->
            if (entries.map { it.file.path }.distinct().size > 1) {
                add(message("health.category.lang"), BitrixHealthIssue(
                    message("health.issue.lang.duplicate", key), entries.firstOrNull()?.file))
            }
        }

        model.modules.forEach { module ->
            val versionFile = module.directory.findFileByRelativePath("install/version.php") ?: return@forEach
            val ownText = runCatching { VfsUtilCore.loadText(versionFile) }.getOrNull() ?: return@forEach
            val ownVersion = VERSION.find(ownText)?.groupValues?.getOrNull(1) ?: return@forEach
            listOf("module.php", "install/index.php").forEach { name ->
                val file = module.directory.findFileByRelativePath(name) ?: return@forEach
                val text = runCatching { VfsUtilCore.loadText(file) }.getOrNull() ?: return@forEach
                val version = MODULE_VERSION.find(text)?.groupValues?.getOrNull(1) ?: return@forEach
                if (version != ownVersion) {
                    add(message("health.category.version"), BitrixHealthIssue(
                        message("health.issue.module.version", module.id, ownVersion, version), file))
                }
            }
        }

        return result.filterValues { it.isNotEmpty() }
    }

    private val VERSION = Regex("""['"]VERSION['"]\s*=>\s*['"]([^'"]+)['"]""")
    private val MODULE_VERSION = Regex("""MODULE_VERSION\s*=\s*['"]([^'"]+)['"]""")
}
