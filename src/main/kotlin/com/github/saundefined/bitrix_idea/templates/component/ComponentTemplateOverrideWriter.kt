package com.github.saundefined.bitrix_idea.templates.component

import com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.io.IOException

/**
 * Копирует шаблон компонента в `local/templates/<шаблон сайта>/components/...`,
 * создавая переопределение вместо правки ядра.
 */
object ComponentTemplateOverrideWriter {
    fun override(
        project: Project,
        componentId: String,
        sourceTemplate: String,
        siteTemplate: String,
        targetTemplate: String,
    ): Result<VirtualFile> {
        if (!componentId.contains(":")) {
            return Result.failure(IllegalArgumentException("Component ID must contain ':'"))
        }
        val componentPath = componentId.replaceFirst(':', '/')
        val sourceName = sourceTemplate.ifBlank { ".default" }
        val targetName = targetTemplate.ifBlank { sourceName }

        val candidates = BitrixProjectStructure.findComponentTemplateDirs(project, componentId, sourceName)
        val source = candidates.firstOrNull { it.path.contains("/bitrix/components/") || it.path.contains("/install/components/") }
            ?: candidates.firstOrNull()
            ?: return Result.failure(IOException("Template $componentId:$sourceName not found"))

        val root = BitrixProjectStructure.bitrixRoot(project)
            ?: return Result.failure(IOException("Project root not found"))
        val local = root.findChild("local") ?: runCatching { root.createChildDirectory(this, "local") }.getOrNull()
            ?: return Result.failure(IOException("Cannot create local directory"))
        val templates = local.findChild("templates") ?: runCatching { local.createChildDirectory(this, "templates") }.getOrNull()
            ?: return Result.failure(IOException("Cannot create local/templates"))
        val site = templates.findChild(siteTemplate)
            ?: runCatching { templates.createChildDirectory(this, siteTemplate) }.getOrNull()
            ?: return Result.failure(IOException("Cannot create site template $siteTemplate"))
        val components = site.findChild("components") ?: runCatching { site.createChildDirectory(this, "components") }.getOrNull()
            ?: return Result.failure(IOException("Cannot create components directory"))

        var parent = components
        componentPath.split('/').forEach { part ->
            parent = parent.findChild(part) ?: runCatching { parent.createChildDirectory(this, part) }.getOrNull()
                ?: throw IOException("Cannot create $part")
        }
        if (parent.findChild(targetName) != null) {
            return Result.failure(IOException("Template ${componentId}:$targetName already exists"))
        }
        val target = parent.createChildDirectory(this, targetName)
        VfsUtil.copyDirectory(this, source, target, null)
        return Result.success(target)
    }
}
