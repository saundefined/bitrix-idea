package com.github.saundefined.bitrix_idea.library

import com.github.saundefined.bitrix_idea.settings.BitrixProjectSettings
import com.github.saundefined.bitrix_idea.structure.BitrixModuleInfo
import com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.AdditionalLibraryRootsListener
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider
import com.intellij.openapi.roots.SyntheticLibrary
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import icons.BitrixIdeaIcons
import java.util.concurrent.ConcurrentHashMap
import javax.swing.Icon

/**
 * Подключает встроенное ядро Bitrix к проекту только при включённой
 * проектной поддержке Bitrix Framework.
 */
class BitrixLibraryRootsProvider : AdditionalLibraryRootsProvider() {
    override fun getAdditionalProjectLibraries(project: Project): Collection<SyntheticLibrary> {
        val root = bundledLibraryRoot ?: return emptyList()
        val sourceRoots = currentSourceRoots(project)
        if (sourceRoots.isEmpty()) return emptyList()
        return listOf(BitrixSyntheticLibrary(root, sourceRoots))
    }

    companion object {
        private val LOG = Logger.getInstance(BitrixLibraryRootsProvider::class.java)
        private val previousRoots = ConcurrentHashMap<Project, List<VirtualFile>>()

        /**
         * Ищет маркеры Bitrix на любой глубине проекта, например `bitrix`,
         * `www/bitrix` или `public/local`.
         */
        fun hasBitrixDirectories(project: Project): Boolean {
            return ReadAction.compute<Boolean, RuntimeException> {
                val root = BitrixProjectStructure.bitrixRoot(project) ?: return@compute false
                if (root.findChild(BITRIX_DIRECTORY)?.isDirectory == true ||
                    root.findChild(LOCAL_DIRECTORY)?.isDirectory == true
                ) {
                    return@compute true
                }
                if (BitrixProjectSettings.getInstance(project).bitrixRootPath.isNotBlank()) {
                    return@compute false
                }

                // Без явно заданного корня сохраняем поиск на любой глубине проекта.
                val scope = GlobalSearchScope.projectScope(project)
                FilenameIndex.getVirtualFilesByName(BITRIX_DIRECTORY, scope).any { it.isDirectory } ||
                    FilenameIndex.getVirtualFilesByName(LOCAL_DIRECTORY, scope).any { it.isDirectory }
            }
        }

        /**
         * Текущие source roots встроенной библиотеки с учётом выключенных модулей.
         */
        fun currentSourceRoots(project: Project): List<VirtualFile> {
            val settings = BitrixProjectSettings.getInstance(project)
            if (!settings.bitrixFrameworkEnabled || !settings.useBundledLibrary) return emptyList()
            val disabled = settings.disabledModules.toSet()
            return bundledModules()
                .filter { it.id !in disabled }
                .map { it.directory }
        }

        /**
         * Заставляет платформу перечитать дополнительные корни библиотеки после смены настройки.
         */
        fun refresh(project: Project) {
            val newRoots = currentSourceRoots(project)
            val oldRoots = previousRoots.put(project, newRoots).orEmpty()
            ApplicationManager.getApplication().runWriteAction {
                AdditionalLibraryRootsListener.fireAdditionalLibraryChanged(
                    project,
                    "Bitrix Idea",
                    oldRoots,
                    newRoots,
                    "Bitrix Idea",
                )
            }
        }

        /**
         * Модули встроенной библиотеки Bitrix. Используются для настроек индексации
         * и автодополнения, даже если у проекта нет собственного каталога `bitrix`.
         */
        fun bundledModules(): List<BitrixModuleInfo> = ReadAction.compute<List<BitrixModuleInfo>, RuntimeException> {
            val root = bundledLibraryRoot ?: return@compute emptyList()
            root.findChild("modules")?.children
                ?.filter { it.isDirectory }
                ?.map { BitrixModuleInfo(it.name, it, local = false, bundled = true) }
                ?.sortedBy { it.id }
                ?: emptyList()
        }

        fun bundledModuleIds(): Set<String> = bundledModules().mapTo(linkedSetOf()) { it.id }

        private val bundledLibraryRoot: VirtualFile? by lazy {
            val url = BitrixLibraryRootsProvider::class.java.classLoader.getResource("bitrix-idea-library")
            if (url == null) {
                LOG.error("Bundled Bitrix library is missing from plugin resources")
                null
            } else {
                ReadAction.compute<VirtualFile?, RuntimeException> { VfsUtil.findFileByURL(url) }
            }
        }

        private const val BITRIX_DIRECTORY = "bitrix"
        private const val LOCAL_DIRECTORY = "local"
    }

    private class BitrixSyntheticLibrary(
        private val root: VirtualFile,
        private val sourceRoots: List<VirtualFile>,
    ) : SyntheticLibrary(), ItemPresentation {
        override fun getSourceRoots(): Collection<VirtualFile> = sourceRoots

        override fun getPresentableText(): String = "Bitrix Idea"

        override fun getIcon(unused: Boolean): Icon = BitrixIdeaIcons.Bitrix

        override fun equals(other: Any?): Boolean {
            return other is BitrixSyntheticLibrary && other.root == root && other.sourceRoots == sourceRoots
        }

        override fun hashCode(): Int = 31 * root.hashCode() + sourceRoots.hashCode()
    }
}
