package com.github.saundefined.bitrix_idea.php

import com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
import com.intellij.navigation.ChooseByNameContributor
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import icons.BitrixIdeaIcons
import javax.swing.Icon

/**
 * Компоненты, шаблоны и модули Bitrix в Search Everywhere / Go to Symbol.
 */
class BitrixChooseByNameContributor : ChooseByNameContributor {
    override fun getNames(project: Project, includeNonProjectItems: Boolean): Array<String> {
        val names = linkedSetOf<String>()
        val model = BitrixProjectStructure.getModel(project)
        model.modules.mapTo(names) { it.id }
        model.components.mapTo(names) { it.id }
        model.siteTemplates.mapTo(names) { it }
        model.langKeys().take(5000).mapTo(names) { it }
        BitrixSettingsEntries.entries(project).map { it.name }.take(5000).mapTo(names) { it }
        model.components.forEach { component ->
            BitrixProjectStructure.availableTemplateNames(project, component.id).forEach { template ->
                names += "${component.id}:$template"
            }
        }
        return names.toTypedArray()
    }

    override fun getItemsByName(
        name: String,
        pattern: String?,
        project: Project,
        includeNonProjectItems: Boolean,
    ): Array<NavigationItem> {
        val model = BitrixProjectStructure.getModel(project)
        val result = mutableListOf<NavigationItem>()

        model.findModule(name)?.let { module ->
            result += BitrixNavigationItem(
                project = project,
                name = module.id,
                location = module.directory.path,
                description = "Bitrix module",
                file = listOf("include.php", ".settings.php", "install/version.php")
                    .firstNotNullOfOrNull { module.directory.findFileByRelativePath(it) }
                    ?: module.directory,
            )
        }

        model.findComponent(name)?.let { component ->
            result += BitrixNavigationItem(
                project = project,
                name = component.id,
                location = component.directory.path,
                description = "Bitrix component",
                file = component.entryFiles().firstOrNull()
                    ?: component.directory.findChild(".description.php")
                    ?: component.directory,
            )
        }

        model.siteTemplates.firstOrNull { it == name }?.let { template ->
            val root = BitrixProjectStructure.bitrixRoot(project)
            val file = root?.findChild("local")?.findChild("templates")?.findChild(template)?.findChild("header.php")
                ?: root?.findChild("bitrix")?.findChild("templates")?.findChild(template)?.findChild("header.php")
            result += BitrixNavigationItem(
                project = project,
                name = template,
                location = file?.path ?: template,
                description = "Bitrix site template",
                file = file,
            )
        }

        model.langEntries[name]?.firstOrNull()?.file?.let { file ->
            result += BitrixNavigationItem(
                project = project,
                name = name,
                location = file.path,
                description = "Bitrix lang message",
                file = file,
            )
        }

        BitrixSettingsEntries.entries(project).filter { it.name == name }.forEach { entry ->
            result += BitrixNavigationItem(
                project = project,
                name = entry.name,
                location = entry.file.path,
                description = entry.description,
                file = entry.file,
            )
        }

        if (name.contains(":")) {
            val componentId = name.substringBeforeLast(':')
            val templateName = name.substringAfterLast(':')
            val component = model.findComponent(componentId)
            if (component != null) {
                val dirs = BitrixProjectStructure.findComponentTemplateDirs(project, componentId, templateName)
                val file = dirs.firstNotNullOfOrNull { it.findChild("template.php") } ?: dirs.firstOrNull()
                result += BitrixNavigationItem(
                    project = project,
                    name = name,
                    location = componentId,
                    description = "Bitrix component template",
                    file = file,
                )
            }
        }

        return result.distinctBy { it.name }.toTypedArray()
    }
}

private class BitrixNavigationItem(
    private val project: Project,
    private val name: String,
    private val location: String,
    private val description: String,
    private val file: VirtualFile?,
) : NavigationItem {
    override fun getName(): String = name

    override fun getPresentation(): ItemPresentation = object : ItemPresentation {
        override fun getPresentableText(): String = name

        override fun getLocationString(): String = "$description · $location"

        override fun getIcon(unused: Boolean): Icon = BitrixIdeaIcons.Bitrix
    }

    override fun navigate(request: Boolean) {
        val virtualFile = file ?: return
        OpenFileDescriptor(project, virtualFile).navigate(request)
    }

    override fun canNavigate(): Boolean = file?.isValid == true

    override fun canNavigateToSource(): Boolean = canNavigate()
}
