package com.github.saundefined.bitrix_idea.structure

import com.github.saundefined.bitrix_idea.settings.BitrixProjectSettings
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import java.io.File
import java.util.Locale

data class BitrixModuleInfo(
    val id: String,
    val directory: VirtualFile,
    val local: Boolean,
    val bundled: Boolean = false,
) {
    val namespace: String
        get() = BitrixModuleNames.namespaceFor(id)
}

data class BitrixComponentInfo(
    val id: String,
    val directory: VirtualFile,
    val local: Boolean,
) {
    val path: String
        get() = id.replaceFirst(':', '/')

    val vendor: String
        get() = id.substringBefore(':')

    fun entryFiles(): List<VirtualFile> = listOfNotNull(
        directory.findChild("class.php"),
        directory.findChild("component.php"),
    )

    fun templateDirs(): List<VirtualFile> = directory.findChild("templates")?.children
        ?.filter { it.isDirectory }
        .orEmpty()
}

data class BitrixLangEntry(
    val file: VirtualFile,
    val key: String,
    val offset: Int,
    val value: String? = null,
)

data class BitrixProjectModel(
    val modules: List<BitrixModuleInfo>,
    val components: List<BitrixComponentInfo>,
    val siteTemplates: List<String>,
    val langEntries: Map<String, List<BitrixLangEntry>>,
    val langFiles: List<VirtualFile>,
) {
    fun findModule(id: String): BitrixModuleInfo? = modules.firstOrNull { it.id == id }

    fun findComponent(id: String): BitrixComponentInfo? = components.firstOrNull { it.id == id }

    fun langKeys(): Set<String> = langEntries.keys
}

object BitrixModuleNames {
    fun namespaceFor(moduleId: String): String {
        val parts = moduleId
            .split('.', '_', '-')
            .filter { it.isNotEmpty() }
            .map { it.replaceFirstChar { c -> c.titlecase(Locale.ROOT) } }
        return if (parts.size == 1) "Bitrix\\${parts.first()}" else parts.joinToString("\\")
    }

    fun constantPrefix(moduleId: String): String = moduleId
        .uppercase(Locale.ROOT)
        .replace(Regex("[^A-Z0-9]+"), "_")
        .trim('_')
}

object BitrixProjectStructure {
    private val MODEL_KEY = Key.create<CachedValue<BitrixProjectModel>>("BitrixProjectModel")
    private val MESS_PATTERN = Regex("""\${'$'}MESS\s*\[\s*['"]([^'"]+)['"]\s*]""")

    fun getModel(project: Project): BitrixProjectModel = ReadAction.compute<BitrixProjectModel, RuntimeException> {
        val manager = CachedValuesManager.getManager(project)
        val settings = BitrixProjectSettings.getInstance(project)
        val settingsTracker = object : ModificationTracker {
            override fun getModificationCount(): Long = settings.settingsRevision
        }
        manager.getCachedValue(
            project,
            MODEL_KEY,
            CachedValueProvider {
                CachedValueProvider.Result.create(
                    scan(project),
                    PsiModificationTracker.MODIFICATION_COUNT,
                    VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS,
                    settingsTracker,
                )
            },
            false,
        )
    }

    /**
     * Каталог, содержащий `bitrix` и `local`. По умолчанию — корень проекта.
     */
    @Suppress("DEPRECATION")
    fun bitrixRoot(project: Project): VirtualFile? {
        val base = project.baseDir ?: return null
        val configured = BitrixProjectSettings.getInstance(project).bitrixRootPath.trim()
        if (configured.isEmpty()) return base
        val file = if (File(configured).isAbsolute) {
            LocalFileSystem.getInstance().findFileByPath(configured)
        } else {
            base.findFileByRelativePath(configured)
        }
        return file ?: base
    }

    private fun isUnderRoot(project: Project, file: VirtualFile): Boolean {
        val root = bitrixRoot(project) ?: return true
        return root == file || VfsUtilCore.isAncestor(root, file, false)
    }

    /**
     * Список всех модулей без учёта фильтра индексации. Нужен странице настроек,
     * чтобы отключённый модуль можно было снова включить.
     */
    fun allModules(project: Project): List<BitrixModuleInfo> = ReadAction.compute<List<BitrixModuleInfo>, RuntimeException> {
        val scope = GlobalSearchScope.projectScope(project)
        val result = mutableListOf<BitrixModuleInfo>()
        FilenameIndex.getVirtualFilesByName("modules", scope).forEach { modulesDir ->
            if (!isUnderRoot(project, modulesDir)) return@forEach
            val parent = modulesDir.parent ?: return@forEach
            if (parent.name != "local" && parent.name != "bitrix") return@forEach
            modulesDir.children.filter { it.isDirectory }.forEach { module ->
                if (result.none { it.id == module.name }) {
                    result += BitrixModuleInfo(module.name, module, parent.name == "local")
                }
            }
        }
        result.sortedBy { it.id }
    }

    fun bitrixVersion(project: Project): String? {
        val file = findRelative(project, "bitrix/modules/main/classes/general/version.php") ?: return null
        val text = runCatching { VfsUtilCore.loadText(file) }.getOrNull() ?: return null
        return Regex("""SM_VERSION['"]?\s*[,=]\s*['"]([^'"]+)['"]""")
            .find(text)
            ?.groupValues
            ?.getOrNull(1)
    }

    fun findLangEntries(project: Project, key: String): List<BitrixLangEntry> {
        return getModel(project).langEntries[key].orEmpty()
    }

    fun hasLangKey(project: Project, key: String): Boolean {
        return getModel(project).langEntries.containsKey(key)
    }

    fun langValue(project: Project, key: String): String? {
        return getModel(project).langEntries[key]?.firstOrNull()?.value
    }

    fun findRelative(project: Project, path: String): VirtualFile? {
        val root = bitrixRoot(project) ?: return null
        root.findFileByRelativePath(path)?.let { return it }

        val name = path.substringAfterLast('/')
        return FilenameIndex.getVirtualFilesByName(name, GlobalSearchScope.projectScope(project))
            .firstOrNull { it.isDirectory && it.path.endsWith("/$path") && isUnderRoot(project, it) }
    }

    fun findComponentTemplateDirs(project: Project, componentId: String, templateName: String): List<VirtualFile> {
        val template = templateName.ifBlank { ".default" }
        val componentPath = componentId.replaceFirst(':', '/')
        val result = mutableListOf<VirtualFile>()

        val model = getModel(project)
        val siteNames = (model.siteTemplates.filter { it != ".default" } + ".default").distinct()

        // Порядок соответствует правилам поиска шаблона Bitrix:
        // local-шаблоны сайта, local/.default, local-компонент,
        // bitrix-шаблоны сайта, bitrix/.default, bitrix-компонент.
        siteNames.forEach { site ->
            findRelative(project, "local/templates/$site/components/$componentPath/$template")
                ?.let { result += it }
        }
        findRelative(project, "local/components/$componentPath/templates/$template")
            ?.let { result += it }

        siteNames.forEach { site ->
            findRelative(project, "bitrix/templates/$site/components/$componentPath/$template")
                ?.let { result += it }
        }
        findRelative(project, "bitrix/components/$componentPath/templates/$template")
            ?.let { result += it }

        // Компонент может лежать в install/components модуля.
        model.findComponent(componentId)
            ?.directory
            ?.findChild("templates")
            ?.findChild(template)
            ?.let { result += it }

        return result.distinct()
    }

    fun availableTemplateNames(project: Project, componentId: String): Set<String> {
        val componentPath = componentId.replaceFirst(':', '/')
        val result = linkedSetOf<String>(".default")
        val model = getModel(project)

        model.findComponent(componentId)?.templateDirs()?.mapTo(result) { it.name }

        val siteNames = (model.siteTemplates.filter { it != ".default" } + ".default").distinct()
        siteNames.forEach { site ->
            findRelative(project, "local/templates/$site/components/$componentPath")
                ?.children
                ?.filter { it.isDirectory }
                ?.mapTo(result) { it.name }
            findRelative(project, "bitrix/templates/$site/components/$componentPath")
                ?.children
                ?.filter { it.isDirectory }
                ?.mapTo(result) { it.name }
        }

        findRelative(project, "local/components/$componentPath/templates")
            ?.children
            ?.filter { it.isDirectory }
            ?.mapTo(result) { it.name }
        findRelative(project, "bitrix/components/$componentPath/templates")
            ?.children
            ?.filter { it.isDirectory }
            ?.mapTo(result) { it.name }

        return result
    }

    /**
     * Возвращает lang-файл, повторяющий путь [sourceFile] для указанного языка, например
     * `.../components/vendor/name/class.php` -> `.../components/vendor/name/lang/ru/class.php`.
     * Если исходный файл уже внутри lang-каталога, возвращается он сам.
     */
    fun findLangTarget(sourceFile: VirtualFile, language: String): VirtualFile? {
        val path = sourceFile.path
        val langSegment = "/lang/"
        val langIndex = path.indexOf(langSegment)
        if (langIndex >= 0) return sourceFile

        val base = findBaseDirectory(sourceFile) ?: sourceFile.parent ?: return null
        val relative = VfsUtilCore.getRelativePath(sourceFile, base, '/') ?: sourceFile.name

        var parent = base
        parent = parent.findChild("lang") ?: return null
        parent = parent.findChild(language) ?: return null
        val parts = relative.split('/').dropLast(1)
        parts.forEach { part ->
            parent = parent?.findChild(part) ?: return null
        }
        return parent?.findChild(sourceFile.name)
    }

    fun findLangTargetParent(sourceFile: VirtualFile, language: String): VirtualFile? {
        val target = findLangTarget(sourceFile, language)
        if (target != null) return target.parent

        val base = findBaseDirectory(sourceFile) ?: sourceFile.parent ?: return null
        val relative = VfsUtilCore.getRelativePath(sourceFile, base, '/') ?: sourceFile.name
        var parent = base.findChild("lang") ?: return null
        parent = parent.findChild(language) ?: return null
        relative.split('/').dropLast(1).forEach { part ->
            parent = parent?.findChild(part) ?: return null
        }
        return parent
    }

    fun langFilesForTarget(sourceFile: VirtualFile, language: String): VirtualFile? {
        return findLangTarget(sourceFile, language)
    }

    fun findOwnerBase(file: VirtualFile): VirtualFile? = findBaseDirectory(file)

    fun companionLangFile(sourceFile: VirtualFile, targetLanguage: String): VirtualFile? {
        val path = sourceFile.path
        val marker = "/lang/"
        val start = path.indexOf(marker)
        if (start < 0) return null
        val languageEnd = path.indexOf('/', start + marker.length)
        if (languageEnd < 0) return null
        val currentLanguage = path.substring(start + marker.length, languageEnd)
        if (currentLanguage == targetLanguage) return sourceFile
        val targetPath = path.substring(0, start) + "/lang/" + targetLanguage + path.substring(languageEnd)
        return com.intellij.openapi.vfs.LocalFileSystem.getInstance().findFileByPath(targetPath)
    }

    fun psiElementAt(project: Project, entry: BitrixLangEntry) = run {
        val file = PsiManager.getInstance(project).findFile(entry.file) ?: return@run null
        file.findElementAt(entry.offset)
    }

    private fun moduleIdFromPath(path: String): String? {
        val marker = "/modules/"
        val start = path.indexOf(marker)
        if (start < 0) return null
        return path.substring(start + marker.length).substringBefore('/')
    }

    private fun findBaseDirectory(file: VirtualFile): VirtualFile? {
        var current: VirtualFile? = file.parent
        while (current != null) {
            val parent = current.parent ?: return null
            when {
                parent.parent?.name == "components" -> return current
                parent.name == "modules" -> return current
                parent.name == "templates" -> return current
                current.name == "local" || current.name == "bitrix" -> return null
            }
            current = parent
        }
        return null
    }

    private fun scan(project: Project): BitrixProjectModel {
        val scope = GlobalSearchScope.projectScope(project)
        val settings = BitrixProjectSettings.getInstance(project)
        val disabledModules = settings.disabledModules.toSet()
        val excludedComponents = settings.excludedComponentIds.toSet()
        val excludedTemplates = settings.excludedSiteTemplates.toSet()
        val modules = mutableListOf<BitrixModuleInfo>()
        val components = linkedMapOf<String, BitrixComponentInfo>()
        val siteTemplates = linkedSetOf<String>()
        val langEntries = mutableMapOf<String, MutableList<BitrixLangEntry>>()
        val langFiles = mutableListOf<VirtualFile>()

        FilenameIndex.getVirtualFilesByName("modules", scope).forEach { modulesDir ->
            if (!isUnderRoot(project, modulesDir)) return@forEach
            val parent = modulesDir.parent ?: return@forEach
            if (parent.name != "local" && parent.name != "bitrix") return@forEach
            val local = parent.name == "local"
            modulesDir.children.filter { it.isDirectory && it.name !in disabledModules }.forEach { module ->
                if (modules.none { it.id == module.name }) {
                    modules += BitrixModuleInfo(module.name, module, local)
                }
            }
        }

        FilenameIndex.getVirtualFilesByName("components", scope).forEach { componentsDir ->
            if (!isUnderRoot(project, componentsDir)) return@forEach
            val path = componentsDir.path
            val direct = componentsDir.parent?.name == "local" || componentsDir.parent?.name == "bitrix"
            val fromModule = path.contains("/install/components")
            if (!direct && !fromModule) return@forEach
            if (fromModule && moduleIdFromPath(path) in disabledModules) return@forEach

            componentsDir.children.filter { it.isDirectory }.forEach { vendor ->
                vendor.children.filter { it.isDirectory }.forEach { component ->
                    val id = "${vendor.name}:${component.name}"
                    if (id in excludedComponents) return@forEach
                    val local = path.contains("/local/")
                    val existing = components[id]
                    if (existing == null || (!existing.local && local)) {
                        components[id] = BitrixComponentInfo(id, component, local)
                    }
                }
            }
        }

        FilenameIndex.getVirtualFilesByName("templates", scope).forEach { templatesDir ->
            if (!isUnderRoot(project, templatesDir)) return@forEach
            val parent = templatesDir.parent ?: return@forEach
            if (parent.name == "local" || parent.name == "bitrix") {
                templatesDir.children
                    .filter { it.isDirectory && it.name !in excludedTemplates }
                    .mapTo(siteTemplates) { it.name }
            }
        }

        FilenameIndex.getVirtualFilesByName("lang", scope).forEach { langDir ->
            if (!isUnderRoot(project, langDir)) return@forEach
            val path = langDir.path
            if (!path.contains("/local/") && !path.contains("/bitrix/")) return@forEach
            if (moduleIdFromPath(path) in disabledModules) return@forEach
            langDir.children.filter { it.isDirectory }.forEach { languageDir ->
                collectLangFiles(languageDir, langEntries, langFiles)
            }
        }

        return BitrixProjectModel(
            modules = modules.sortedBy { it.id },
            components = components.values.sortedBy { it.id },
            siteTemplates = siteTemplates.sorted(),
            langEntries = langEntries,
            langFiles = langFiles,
        )
    }

    private fun collectLangFiles(
        directory: VirtualFile,
        entries: MutableMap<String, MutableList<BitrixLangEntry>>,
        files: MutableList<VirtualFile>,
    ) {
        directory.children.forEach { child ->
            if (child.isDirectory) {
                collectLangFiles(child, entries, files)
            } else if (child.extension.equals("php", ignoreCase = true)) {
                files += child
                val text = runCatching { VfsUtilCore.loadText(child) }.getOrNull() ?: return@forEach
                MESS_PATTERN.findAll(text).forEach { match ->
                    val key = match.groupValues[1]
                    if (key.isBlank()) return@forEach
                    val offset = match.groups[1]?.range?.first ?: return@forEach
                    val value = extractLangValue(text, match.range.last + 1)
                    entries.getOrPut(key) { mutableListOf() } += BitrixLangEntry(child, key, offset, value)
                }
            }
        }
    }

    private fun extractLangValue(text: String, start: Int): String? {
        var index = start
        while (index < text.length && text[index] != '=') index++
        while (index < text.length && text[index] != '\'' && text[index] != '"') index++
        if (index >= text.length) return null

        val quote = text[index]
        index++
        val value = StringBuilder()
        var escaped = false
        while (index < text.length) {
            val char = text[index]
            if (escaped) {
                value.append(char)
                escaped = false
            } else if (char == '\\') {
                escaped = true
            } else if (char == quote) {
                return value.toString()
            } else {
                value.append(char)
            }
            index++
        }
        return null
    }
}
