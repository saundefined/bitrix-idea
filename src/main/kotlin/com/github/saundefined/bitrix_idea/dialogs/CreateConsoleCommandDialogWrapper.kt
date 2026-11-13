package com.github.saundefined.bitrix_idea.dialogs

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.orm.OrmEntityContextResolver
import com.github.saundefined.bitrix_idea.structure.BitrixModuleNames
import com.github.saundefined.bitrix_idea.templates.d7.ConsoleCommandGenerator
import com.github.saundefined.bitrix_idea.templates.d7.D7ClassType
import com.github.saundefined.bitrix_idea.templates.d7.D7SettingsRegistrar
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiDirectory
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class CreateConsoleCommandDialogWrapper(
    private val project: Project,
    private val directory: PsiDirectory,
) : DialogWrapper(project, true) {
    private val moduleRootPath = OrmEntityContextResolver.resolveModuleRoot(directory.virtualFile.path)
    private val moduleId = OrmEntityContextResolver.resolveModuleId(directory.virtualFile.path).orEmpty()
    private val moduleNamespace = BitrixModuleNames.namespaceFor(moduleId)

    private var className: String = "Command"
    private var commandName: String = moduleId.replace('.', ':') + ":run"
    private var description: String = "Команда модуля"
    private var arguments: String = ""
    private var register: Boolean = true

    private val contentPanel = panel {
        row(message("create.console.class")) {
            textField()
                .bindText(::className)
                .focused()
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row(message("create.console.name")) {
            textField()
                .bindText(::commandName)
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row(message("create.console.description")) {
            textField()
                .bindText(::description)
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row(message("create.console.arguments")) {
            textField()
                .bindText(::arguments)
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row {
            checkBox(message("create.console.register"))
                .bindSelected(::register)
        }
    }

    init {
        init()
        title = message("create.console.title")
    }

    override fun createCenterPanel(): JComponent = contentPanel

    override fun doValidate(): ValidationInfo? {
        if (!className.matches(Regex("^[A-Z][A-Za-z0-9]*$"))) {
            return ValidationInfo(message("create.console.class.validation"), null)
        }
        if (commandName.isBlank()) return ValidationInfo(message("create.console.name.validation"), null)
        return null
    }

    override fun doOKAction() {
        val root = moduleRootPath?.let { LocalFileSystem.getInstance().findFileByPath(it) }
        if (root == null || moduleId.isBlank()) {
            Messages.showErrorDialog(project, message("create.admin.module.not.found"), title)
            return
        }
        val namespace = "$moduleNamespace\\Console"
        val args = arguments.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val text = ConsoleCommandGenerator.generate(namespace, className, commandName, description, args)
        if (!GeneratorPreviewDialogWrapper(project, listOf(GeneratorPreviewFile("lib/Console/$className.php", text))).showAndGet()) return

        WriteCommandAction.runWriteCommandAction(project) {
            val lib = root.findChild("lib") ?: root.createChildDirectory(this, "lib")
            val console = lib.findChild("Console") ?: lib.createChildDirectory(this, "Console")
            val file = console.findChild("$className.php") ?: console.createChildData(this, "$className.php")
            VfsUtil.saveText(file, text)

            if (register) {
                D7SettingsRegistrar.register(
                    moduleRoot = root,
                    type = D7ClassType.CONSOLE,
                    namespace = namespace,
                    className = className,
                )
            }
        }
        super.doOKAction()
    }
}
