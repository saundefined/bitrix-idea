package com.github.saundefined.bitrix_idea.dialogs

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.templates.d7.AjaxActionGenerator
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

/**
 * Добавляет метод действия и запись в `configureActions` D7-контроллера.
 */
class AddAjaxActionDialogWrapper(
    private val project: Project,
    private val file: PsiFile,
) : DialogWrapper(project, true) {
    private var actionName: String = "list"
    private var parameters: String = ""
    private var prefilters: String = ""

    private val contentPanel = panel {
        row(message("add.ajax.action.name")) {
            textField()
                .bindText(::actionName)
                .focused()
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row(message("add.ajax.action.parameters")) {
            textField()
                .bindText(::parameters)
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row(message("add.ajax.action.filters")) {
            textField()
                .bindText(::prefilters)
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row {
            comment(message("add.ajax.action.description"))
        }
    }

    init {
        init()
        title = message("add.ajax.action.title")
    }

    override fun createCenterPanel(): JComponent = contentPanel

    override fun doValidate(): ValidationInfo? {
        val name = actionName.trim().removeSuffix("Action")
        if (!name.matches(Regex("^[a-zA-Z_][A-Za-z0-9_]*$"))) {
            return ValidationInfo(message("add.ajax.action.validation.name"), null)
        }
        val params = parameters.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        if (params.any { !it.matches(Regex("^[a-zA-Z_][A-Za-z0-9_]*$")) }) {
            return ValidationInfo(message("add.ajax.action.validation.parameters"), null)
        }
        return null
    }

    override fun doOKAction() {
        val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return
        val params = parameters.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val filters = prefilters.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val updated = AjaxActionGenerator.addAction(document.text, actionName, params, filters)
        if (!GeneratorPreviewDialogWrapper(
                project,
                listOf(GeneratorPreviewFile(file.name, updated)),
            ).showAndGet()
        ) {
            return
        }

        WriteCommandAction.runWriteCommandAction(project) {
            document.setText(updated)
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }
        super.doOKAction()
    }
}
