package com.github.saundefined.bitrix_idea.dialogs

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.orm.OrmEntityContextResolver
import com.github.saundefined.bitrix_idea.settings.AppSettingsState
import com.github.saundefined.bitrix_idea.templates.module.AdminPageGenerator
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

/**
 * Генератор admin-страницы модуля и `admin/menu.php`.
 */
class CreateAdminPageDialogWrapper(
    private val project: Project,
    private val directory: PsiDirectory,
) : DialogWrapper(project, true) {
    private val moduleRootPath = OrmEntityContextResolver.resolveModuleRoot(directory.virtualFile.path)
    private val moduleId = OrmEntityContextResolver.resolveModuleId(directory.virtualFile.path).orEmpty()

    private var pageFile: String = "settings.php"
    private var pageTitle: String = "Настройки"
    private var parentMenu: String = "global_menu_content"
    private var sort: String = "100"
    private var addMenu: Boolean = true
    private var addLang: Boolean = true

    private val contentPanel = panel {
        row(message("create.admin.module")) {
            label(moduleId.ifBlank { "—" })
        }
        row(message("create.admin.page")) {
            textField()
                .bindText(::pageFile)
                .focused()
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row(message("create.admin.title")) {
            textField()
                .bindText(::pageTitle)
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row(message("create.admin.parent")) {
            textField()
                .bindText(::parentMenu)
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row(message("create.admin.sort")) {
            textField()
                .bindText(::sort)
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row {
            checkBox(message("create.admin.add.menu"))
                .bindSelected(::addMenu)
            checkBox(message("create.admin.add.lang"))
                .bindSelected(::addLang)
        }
    }

    init {
        init()
        title = message("create.admin.title.dialog")
    }

    override fun createCenterPanel(): JComponent = contentPanel

    override fun doValidate(): ValidationInfo? {
        contentPanel.apply()
        if (!pageFile.matches(Regex("^[A-Za-z0-9_.-]+\\.php$"))) {
            return ValidationInfo(message("create.admin.page.validation"), null)
        }
        if (pageTitle.isBlank()) return ValidationInfo(message("create.admin.title.validation"), null)
        if (sort.trim().toIntOrNull() == null) return ValidationInfo(message("create.admin.sort.validation"), null)
        return null
    }

    override fun doOKAction() {
        val root = moduleRootPath?.let { LocalFileSystem.getInstance().findFileByPath(it) }
        if (root == null || moduleId.isBlank()) {
            Messages.showErrorDialog(project, message("create.admin.module.not.found"), title)
            return
        }
        if (root.findChild("admin")?.findChild(pageFile) != null) {
            val result = Messages.showYesNoDialog(
                project,
                message("create.admin.page.exists"),
                title,
                Messages.getQuestionIcon(),
            )
            if (result != Messages.YES) return
        }

        val pageText = AdminPageGenerator.generatePage(moduleId, pageFile, pageTitle)
        val titleKey = AdminPageGenerator.pageTitleKey(pageFile)
        val menuText = AdminPageGenerator.generateMenu(
            moduleId = moduleId,
            pageFile = pageFile,
            title = pageTitle,
            parentMenu = parentMenu.trim().ifBlank { "global_menu_content" },
            sort = sort.trim().toIntOrNull() ?: 100,
            section = "menu_" + moduleId.replace(Regex("[^a-zA-Z0-9_]+"), "_"),
        )
        val languages = if (addLang) AppSettingsState.getInstance().languages.ifEmpty { listOf("ru") } else emptyList()

        val preview = buildList {
            add(GeneratorPreviewFile("admin/$pageFile", pageText))
            if (addMenu) add(GeneratorPreviewFile("admin/menu.php", menuText))
            languages.forEach { language ->
                add(GeneratorPreviewFile("lang/$language/admin/$pageFile", AdminPageGenerator.generatePageLang(pageTitle, titleKey)))
                if (addMenu) {
                    add(GeneratorPreviewFile("lang/$language/admin/menu.php", AdminPageGenerator.generateMenuLang(pageTitle)))
                }
            }
        }
        if (!GeneratorPreviewDialogWrapper(project, preview).showAndGet()) return

        WriteCommandAction.runWriteCommandAction(project) {
            val admin = root.findChild("admin") ?: root.createChildDirectory(this, "admin")
            val page = admin.findChild(pageFile) ?: admin.createChildData(this, pageFile)
            VfsUtil.saveText(page, pageText)
            if (addMenu) {
                val menu = admin.findChild("menu.php") ?: admin.createChildData(this, "menu.php")
                VfsUtil.saveText(menu, menuText)
            }
            if (addLang) {
                writeLang(root, languages, pageFile, pageTitle, titleKey, addMenu)
            }
        }
        super.doOKAction()
    }

    private fun writeLang(
        root: VirtualFile,
        languages: List<String>,
        pageFile: String,
        title: String,
        titleKey: String,
        withMenu: Boolean,
    ) {
        val langRoot = root.findChild("lang") ?: root.createChildDirectory(this, "lang")
        languages.forEach { language ->
            val adminLang = langRoot.findChild(language)?.findChild("admin")
                ?: langRoot.findChild(language)?.createChildDirectory(this, "admin")
                ?: langRoot.createChildDirectory(this, language).createChildDirectory(this, "admin")
            val pageLang = adminLang.findChild(pageFile) ?: adminLang.createChildData(this, pageFile)
            VfsUtil.saveText(pageLang, AdminPageGenerator.generatePageLang(title, titleKey))
            if (withMenu) {
                val menuLang = adminLang.findChild("menu.php") ?: adminLang.createChildData(this, "menu.php")
                VfsUtil.saveText(menuLang, AdminPageGenerator.generateMenuLang(title))
            }
        }
    }
}
