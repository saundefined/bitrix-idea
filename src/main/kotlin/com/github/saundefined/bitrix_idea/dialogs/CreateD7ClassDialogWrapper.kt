package com.github.saundefined.bitrix_idea.dialogs

import com.intellij.openapi.command.WriteCommandAction

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.orm.OrmEntityContextResolver
import com.github.saundefined.bitrix_idea.templates.d7.BitrixD7ClassGenerator
import com.github.saundefined.bitrix_idea.templates.d7.D7ClassType
import com.github.saundefined.bitrix_idea.templates.d7.D7SettingsRegistrar
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiDirectory
import com.intellij.util.ui.FormBuilder
import java.awt.Dimension
import java.util.Locale
import javax.swing.JComponent
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.SimpleListCellRenderer

class CreateD7ClassDialogWrapper(
    private val project: Project,
    private val directory: PsiDirectory,
) : DialogWrapper(project, true) {
    private val classNameField = JBTextField()
    private val namespaceField = JBTextField(OrmEntityContextResolver.resolvePath(directory.virtualFile.path).namespace)
    private val typeCombo = ComboBox(D7ClassType.entries.toTypedArray())
    private val registerCheck = JBCheckBox(message("create.d7.register"), true)

    private val contentPanel = FormBuilder.createFormBuilder()
        .addLabeledComponent(message("create.d7.type"), typeCombo)
        .addLabeledComponent(message("create.d7.class"), classNameField)
        .addLabeledComponent(message("create.d7.namespace"), namespaceField)
        .addComponent(registerCheck)
        .panel
        .apply { preferredSize = Dimension(520, 180) }

    init {
        typeCombo.renderer = SimpleListCellRenderer.create("") { value: D7ClassType -> value.displayName }
        typeCombo.addActionListener { updateRegisterAvailability() }
        init()
        title = message("create.d7.title")
        updateRegisterAvailability()
    }

    private fun updateRegisterAvailability() {
        val type = typeCombo.selectedItem as? D7ClassType ?: D7ClassType.SERVICE
        registerCheck.isEnabled = type == D7ClassType.CONTROLLER ||
            type == D7ClassType.AJAX_CONTROLLER ||
            type == D7ClassType.SERVICE ||
            type == D7ClassType.CONSOLE
        if (!registerCheck.isEnabled) registerCheck.isSelected = false
    }

    override fun createCenterPanel(): JComponent = contentPanel

    override fun doValidate(): ValidationInfo? {
        val className = classNameField.text.trim()
        if (!className.matches(Regex("^[A-Z][A-Za-z0-9]*$"))) {
            return ValidationInfo(message("create.d7.class.validation"), classNameField)
        }
        return null
    }

    override fun doOKAction() {
        val className = classNameField.text.trim()
        val fileName = "$className.php"
        if (directory.virtualFile.findChild(fileName) != null) {
            Messages.showErrorDialog(project, message("create.d7.file.exists"), message("create.d7.title"))
            return
        }

        val type = typeCombo.selectedItem as? D7ClassType ?: D7ClassType.SERVICE
        val text = BitrixD7ClassGenerator.generate(
            type = type,
            namespace = namespaceField.text.trim(),
            className = className,
        )
        if (!GeneratorPreviewDialogWrapper(project, listOf(GeneratorPreviewFile(fileName, text))).showAndGet()) return
        WriteCommandAction.runWriteCommandAction(project) {
            val file = directory.virtualFile.createChildData(this, fileName)
            VfsUtil.saveText(file, text)

            if (registerCheck.isSelected && registerCheck.isEnabled) {
                val moduleRootPath = OrmEntityContextResolver.resolvePath(directory.virtualFile.path).moduleRoot
                val moduleRoot = moduleRootPath?.let { LocalFileSystem.getInstance().findFileByPath(it) }
                if (moduleRoot != null) {
                    D7SettingsRegistrar.register(
                        moduleRoot = moduleRoot,
                        type = type,
                        namespace = namespaceField.text.trim(),
                        className = className,
                    )
                }
            }
        }
        super.doOKAction()
    }
}