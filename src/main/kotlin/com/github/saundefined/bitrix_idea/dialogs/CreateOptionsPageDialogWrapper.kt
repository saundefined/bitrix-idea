package com.github.saundefined.bitrix_idea.dialogs

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.orm.OrmEntityContextResolver
import com.github.saundefined.bitrix_idea.settings.AppSettingsState
import com.github.saundefined.bitrix_idea.templates.module.ModuleOptionType
import com.github.saundefined.bitrix_idea.templates.module.ModuleOptionSpec
import com.github.saundefined.bitrix_idea.templates.module.OptionsPageGenerator
import com.github.saundefined.bitrix_idea.ui.ModuleOptionsTable
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiDirectory
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

/**
 * Генератор страницы настроек модуля `options.php`.
 */
class CreateOptionsPageDialogWrapper(
    private val project: Project,
    private val directory: PsiDirectory,
) : DialogWrapper(project, true) {
    private val moduleRootPath = OrmEntityContextResolver.resolveModuleRoot(directory.virtualFile.path)
    private val moduleId = OrmEntityContextResolver.resolveModuleId(directory.virtualFile.path).orEmpty()
    private val table = ModuleOptionsTable(
        listOf(
            ModuleOptionSpec("enabled", "Включено", ModuleOptionType.CHECKBOX, "Y"),
            ModuleOptionSpec("api_key", "API-ключ", ModuleOptionType.TEXT),
        ),
    )

    private val contentPanel = panel {
        row {
            comment(message("create.options.description", moduleId.ifBlank { "—" }))
        }
        row {
            cell(table.component)
                .align(AlignX.FILL)
        }
    }

    init {
        init()
        title = message("create.options.title")
    }

    override fun createCenterPanel(): JComponent = contentPanel

    override fun doOKAction() {
        val root = moduleRootPath?.let { LocalFileSystem.getInstance().findFileByPath(it) }
        if (root == null || moduleId.isBlank()) {
            Messages.showErrorDialog(project, message("create.options.module.not.found"), title)
            return
        }
        if (root.findChild("options.php") != null) {
            val result = Messages.showYesNoDialog(
                project,
                message("create.options.exists"),
                title,
                Messages.getQuestionIcon(),
            )
            if (result != Messages.YES) return
        }

        val options = table.items
        if (options.isEmpty()) {
            Messages.showErrorDialog(project, message("create.options.empty"), title)
            return
        }

        val languages = AppSettingsState.getInstance().languages.ifEmpty { listOf("ru") }
        val preview = buildList {
            add(GeneratorPreviewFile("options.php", OptionsPageGenerator.generate(moduleId, options)))
            languages.forEach { language ->
                add(GeneratorPreviewFile("lang/$language/options.php", OptionsPageGenerator.generateLang(options)))
            }
        }
        if (!GeneratorPreviewDialogWrapper(project, preview).showAndGet()) return

        WriteCommandAction.runWriteCommandAction(project) {
            val file = root.findChild("options.php") ?: root.createChildData(this, "options.php")
            VfsUtil.saveText(file, OptionsPageGenerator.generate(moduleId, options))

            val langRoot = root.findChild("lang") ?: root.createChildDirectory(this, "lang")
            languages.forEach { language ->
                val langDir = langRoot.findChild(language) ?: langRoot.createChildDirectory(this, language)
                val langFile = langDir.findChild("options.php") ?: langDir.createChildData(this, "options.php")
                VfsUtil.saveText(langFile, OptionsPageGenerator.generateLang(options))
            }
        }
        super.doOKAction()
    }
}
