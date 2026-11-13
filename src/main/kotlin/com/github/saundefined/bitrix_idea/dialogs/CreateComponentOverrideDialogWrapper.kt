package com.github.saundefined.bitrix_idea.dialogs

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
import com.github.saundefined.bitrix_idea.templates.component.ComponentTemplateOverrideWriter
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.psi.PsiDirectory
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

/**
 * Создаёт переопределение шаблона компонента в `local/templates/...`.
 */
class CreateComponentOverrideDialogWrapper(
    private val project: Project,
    private val directory: PsiDirectory? = null,
) : DialogWrapper(project, true) {
    private var componentId: String = ""
    private var sourceTemplate: String = ".default"
    private var targetTemplate: String = ".default"
    private var siteTemplate: String = ".default"

    private val siteCombo = ComboBox<String>()

    private val componentIdField = JBTextField(componentId)
    private val sourceTemplateField = JBTextField(sourceTemplate)
    private val targetTemplateField = JBTextField(targetTemplate)
    private val contentPanel = panel {
        row(message("create.override.component")) {
            cell(componentIdField)
                .focused()
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row(message("create.override.source.template")) {
            cell(sourceTemplateField)
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row(message("create.override.target.template")) {
            cell(targetTemplateField)
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row(message("create.override.site.template")) {
            cell(siteCombo)
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row {
            comment(message("create.override.description"))
        }
    }

    init {
        val templates = BitrixProjectStructure.getModel(project).siteTemplates
            .filter { it != ".default" }
        siteCombo.isEditable = true
        siteCombo.addItem(".default")
        templates.forEach { siteCombo.addItem(it) }
        siteCombo.selectedItem = templates.firstOrNull() ?: ".default"
        siteCombo.addActionListener {
            siteTemplate = (siteCombo.selectedItem as? String).orEmpty()
        }

        directory?.let { selected ->
            val virtualFile = selected.virtualFile
            val components = virtualFile.parent
            if (virtualFile.name != "components" && components?.name == "components") {
                componentId = "${virtualFile.parent?.name}:${virtualFile.name}"
                componentIdField.text = componentId
            }
        }

        init()
        title = message("create.override.title")
    }

    override fun createCenterPanel(): JComponent = contentPanel

    override fun doValidate(): ValidationInfo? {
        componentId = componentIdField.text
        sourceTemplate = sourceTemplateField.text
        targetTemplate = targetTemplateField.text
        contentPanel.apply()
        if (!componentId.matches(Regex("^[A-Za-z0-9_.-]+:[A-Za-z0-9_.-]+$"))) {
            return ValidationInfo(message("create.override.component.validation"), null)
        }
        if (sourceTemplate.isBlank() || targetTemplate.isBlank()) {
            return ValidationInfo(message("create.override.template.validation"), null)
        }
        return null
    }

    override fun doOKAction() {
        componentId = componentIdField.text
        sourceTemplate = sourceTemplateField.text
        targetTemplate = targetTemplateField.text
        val selectedSite = (siteCombo.selectedItem as? String)?.trim().orEmpty()
        if (selectedSite.isBlank()) {
            Messages.showErrorDialog(project, message("create.override.site.validation"), title)
            return
        }
        val result = runCatching {
            ApplicationManager.getApplication().runWriteAction<Result<com.intellij.openapi.vfs.VirtualFile>> {
                ComponentTemplateOverrideWriter.override(
                    project = project,
                    componentId = componentId.trim(),
                    sourceTemplate = sourceTemplate.trim(),
                    siteTemplate = selectedSite,
                    targetTemplate = targetTemplate.trim(),
                )
            }
        }.getOrElse { Result.failure(it) }
        if (result.isFailure) {
            Messages.showErrorDialog(
                project,
                message("create.override.error", result.exceptionOrNull()?.message ?: ""),
                title,
            )
            return
        }
        val target = result.getOrNull()
        super.doOKAction()
        target?.findChild("template.php")?.let { file ->
            OpenFileDescriptor(project, file).navigate(true)
        }
    }
}
