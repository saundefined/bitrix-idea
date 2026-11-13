package com.github.saundefined.bitrix_idea.dialogs

import com.intellij.openapi.command.WriteCommandAction

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.orm.OrmEntityContextResolver
import com.github.saundefined.bitrix_idea.php.BitrixEventNames
import com.github.saundefined.bitrix_idea.templates.d7.D7SettingsRegistrar
import com.github.saundefined.bitrix_idea.templates.events.HandlerGenerator
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
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
import java.util.Locale
import javax.swing.JComponent

/**
 * Создаёт класс обработчика события и при необходимости регистрирует его в `.settings.php`.
 */
class CreateEventHandlerDialogWrapper(
    private val project: Project,
    private val directory: PsiDirectory,
) : DialogWrapper(project, true) {
    private var eventName: String = "OnBeforeProlog"
    private var className: String = "Handlers"
    private var methodName: String = "onBeforeProlog"
    private var register: Boolean = true

    private val eventCombo = ComboBox<String>()
    private val methodField = JBTextField(methodName)
    private val modulePath = OrmEntityContextResolver.resolvePath(directory.virtualFile.path)
    private val moduleId = moduleIdFromPath(directory.virtualFile.path)
    private val namespace = modulePath.namespace

    private val contentPanel = panel {
        row(message("create.event.module")) {
            label(moduleId ?: message("create.event.module.not.found"))
        }
        row(message("create.event.event")) {
            cell(eventCombo)
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row(message("create.event.class")) {
            textField()
                .bindText(::className)
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row(message("create.event.method")) {
            cell(methodField)
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row {
            checkBox(message("create.event.register"))
                .bindSelected(::register)
                .enabled(modulePath.moduleRoot != null && moduleId != null)
        }
        row {
            comment(message("create.event.namespace", namespace.ifBlank { "—" }))
        }
    }

    init {
        eventCombo.isEditable = true
        BitrixEventNames.forModule(moduleId).forEach { eventCombo.addItem(it) }
        eventCombo.selectedItem = eventName
        var autoMethod = methodName
        eventCombo.addActionListener {
            val selected = (eventCombo.selectedItem as? String)?.trim().orEmpty()
            if (selected.isNotBlank()) {
                eventName = selected
                if (methodField.text.isBlank() || methodField.text == autoMethod) {
                    methodName = methodNameFor(selected)
                    methodField.text = methodName
                    autoMethod = methodName
                }
            }
        }
        init()
        title = message("create.event.title")
    }

    override fun createCenterPanel(): JComponent = contentPanel

    override fun doValidate(): ValidationInfo? {
        val selectedEvent = (eventCombo.selectedItem as? String)?.trim().orEmpty()
        if (selectedEvent.isBlank()) return ValidationInfo(message("create.event.validation.event"), eventCombo)
        if (!className.matches(Regex("^[A-Z][A-Za-z0-9]*$"))) {
            return ValidationInfo(message("create.event.validation.class"), null)
        }
        methodName = methodField.text.trim()
        if (!methodName.matches(Regex("^[a-zA-Z_][A-Za-z0-9_]*$"))) {
            return ValidationInfo(message("create.event.validation.method"), methodField)
        }
        return null
    }

    override fun doOKAction() {
        val selectedEvent = (eventCombo.selectedItem as? String)?.trim().orEmpty()
        methodName = methodField.text.trim()
        val fileName = "$className.php"
        if (directory.virtualFile.findChild(fileName) != null) {
            Messages.showErrorDialog(project, message("create.event.file.exists"), title)
            return
        }

        val text = HandlerGenerator.eventHandler(namespace, className, methodName, selectedEvent)
        if (!GeneratorPreviewDialogWrapper(project, listOf(GeneratorPreviewFile(fileName, text))).showAndGet()) return
        WriteCommandAction.runWriteCommandAction(project) {
            VfsUtil.saveText(directory.virtualFile.createChildData(this, fileName), text)
            if (register && modulePath.moduleRoot != null && moduleId != null) {
                val moduleRoot = LocalFileSystem.getInstance().findFileByPath(modulePath.moduleRoot)
                if (moduleRoot != null) {
                    val handlerFqn = if (namespace.isBlank()) "\\$className" else "\\$namespace\\$className"
                    D7SettingsRegistrar.registerEvent(moduleRoot, moduleId, selectedEvent, handlerFqn, methodName)
                }
            }
        }
        super.doOKAction()
    }

    private fun moduleIdFromPath(path: String): String? {
        val segments = path.split('/').filter { it.isNotEmpty() }
        val index = segments.indexOfLast { it == "modules" }
        return if (index >= 0) segments.getOrNull(index + 1) else null
    }

    private fun methodNameFor(event: String): String {
        if (event.startsWith("On") && event.length > 2) {
            return event.substring(2).replaceFirstChar { it.lowercase(Locale.ROOT) }
        }
        return event.replaceFirstChar { it.lowercase(Locale.ROOT) }
    }
}