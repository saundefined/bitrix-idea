package com.github.saundefined.bitrix_idea.dialogs

import com.intellij.openapi.command.WriteCommandAction

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.orm.OrmEntityContextResolver
import com.github.saundefined.bitrix_idea.templates.d7.D7ClassType
import com.github.saundefined.bitrix_idea.templates.d7.D7SettingsRegistrar
import com.github.saundefined.bitrix_idea.templates.d7.ServiceGenerator
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiDirectory
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

/**
 * Создаёт интерфейс сервиса и его реализацию с регистрацией в `.settings.php`.
 */
class CreateServiceDialogWrapper(
    private val project: Project,
    private val directory: PsiDirectory,
) : DialogWrapper(project, true) {
    private var interfaceName: String = "ServiceInterface"
    private var className: String = "Service"
    private var register: Boolean = true

    private val modulePath = OrmEntityContextResolver.resolvePath(directory.virtualFile.path)
    private val namespace = modulePath.namespace

    private val contentPanel = panel {
        row(message("create.service.interface")) {
            textField()
                .bindText(::interfaceName)
                .focused()
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row(message("create.service.class")) {
            textField()
                .bindText(::className)
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row {
            checkBox(message("create.service.register"))
                .bindSelected(::register)
                .enabled(modulePath.moduleRoot != null)
        }
        row {
            comment(message("create.event.namespace", namespace.ifBlank { "—" }))
        }
    }

    init {
        init()
        title = message("create.service.title")
    }

    override fun createCenterPanel(): JComponent = contentPanel

    override fun doValidate(): ValidationInfo? {
        contentPanel.apply()
        if (!interfaceName.matches(Regex("^[A-Z][A-Za-z0-9]*$"))) {
            return ValidationInfo(message("create.service.validation.interface"), null)
        }
        if (!className.matches(Regex("^[A-Z][A-Za-z0-9]*$"))) {
            return ValidationInfo(message("create.service.validation.class"), null)
        }
        return null
    }

    override fun doOKAction() {
        val interfaceFile = "$interfaceName.php"
        val classFile = "$className.php"
        if (directory.virtualFile.findChild(interfaceFile) != null || directory.virtualFile.findChild(classFile) != null) {
            Messages.showErrorDialog(project, message("create.event.file.exists"), title)
            return
        }

        val interfaceText = ServiceGenerator.generateInterface(namespace, interfaceName)
        val classText = ServiceGenerator.generateImplementation(namespace, interfaceName, className)
        val preview = listOf(
            GeneratorPreviewFile(interfaceFile, interfaceText),
            GeneratorPreviewFile(classFile, classText),
        )
        if (!GeneratorPreviewDialogWrapper(project, preview).showAndGet()) return

        WriteCommandAction.runWriteCommandAction(project) {
            VfsUtil.saveText(
                directory.virtualFile.createChildData(this, interfaceFile),
                interfaceText,
            )
            VfsUtil.saveText(
                directory.virtualFile.createChildData(this, classFile),
                classText,
            )

            if (register && modulePath.moduleRoot != null) {
                val moduleRoot = LocalFileSystem.getInstance().findFileByPath(modulePath.moduleRoot)
                if (moduleRoot != null) {
                    D7SettingsRegistrar.register(
                        moduleRoot = moduleRoot,
                        type = D7ClassType.SERVICE,
                        namespace = namespace,
                        className = className,
                    )
                }
            }
        }
        super.doOKAction()
    }
}