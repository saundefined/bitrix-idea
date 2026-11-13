package com.github.saundefined.bitrix_idea.dialogs

import com.intellij.openapi.command.WriteCommandAction

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.orm.OrmEntityContextResolver
import com.github.saundefined.bitrix_idea.templates.d7.D7SettingsRegistrar
import com.github.saundefined.bitrix_idea.templates.events.HandlerGenerator
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiDirectory
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

/**
 * Создаёт агент и добавляет его регистрацию в `.settings.php`.
 */
class CreateAgentDialogWrapper(
    private val project: Project,
    private val directory: PsiDirectory,
) : DialogWrapper(project, true) {
    private var className: String = "Agent"
    private var methodName: String = "run"
    private var interval: String = "86400"
    private var register: Boolean = true

    private val modulePath = OrmEntityContextResolver.resolvePath(directory.virtualFile.path)
    private val namespace = modulePath.namespace

    private val contentPanel = panel {
        row(message("create.agent.class")) {
            textField()
                .bindText(::className)
                .focused()
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row(message("create.agent.method")) {
            textField()
                .bindText(::methodName)
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row(message("create.agent.interval")) {
            textField()
                .bindText(::interval)
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row {
            checkBox(message("create.agent.register"))
                .bindSelected(::register)
                .enabled(modulePath.moduleRoot != null)
        }
        row {
            comment(message("create.agent.namespace", namespace.ifBlank { "—" }))
        }
    }

    init {
        init()
        title = message("create.agent.title")
    }

    override fun createCenterPanel(): JComponent = contentPanel

    override fun doValidate(): ValidationInfo? {
        if (!className.matches(Regex("^[A-Z][A-Za-z0-9]*$"))) {
            return ValidationInfo(message("create.event.validation.class"), null)
        }
        if (!methodName.matches(Regex("^[a-zA-Z_][A-Za-z0-9_]*$"))) {
            return ValidationInfo(message("create.event.validation.method"), null)
        }
        if (interval.trim().toIntOrNull() == null || interval.trim().toInt() <= 0) {
            return ValidationInfo(message("create.agent.validation.interval"), null)
        }
        return null
    }

    override fun doOKAction() {
        val fileName = "$className.php"
        if (directory.virtualFile.findChild(fileName) != null) {
            Messages.showErrorDialog(project, message("create.event.file.exists"), title)
            return
        }
        val text = HandlerGenerator.agent(namespace, className, methodName)
        if (!GeneratorPreviewDialogWrapper(project, listOf(GeneratorPreviewFile(fileName, text))).showAndGet()) return
        WriteCommandAction.runWriteCommandAction(project) {
            VfsUtil.saveText(directory.virtualFile.createChildData(this, fileName), text)
            if (register && modulePath.moduleRoot != null) {
                val moduleRoot = LocalFileSystem.getInstance().findFileByPath(modulePath.moduleRoot)
                if (moduleRoot != null) {
                    val fqn = if (namespace.isBlank()) "\\$className" else "\\$namespace\\$className"
                    D7SettingsRegistrar.registerAgent(
                        moduleRoot = moduleRoot,
                        callable = "$fqn::$methodName",
                        intervalSeconds = interval.trim().toInt(),
                    )
                }
            }
        }
        super.doOKAction()
    }
}