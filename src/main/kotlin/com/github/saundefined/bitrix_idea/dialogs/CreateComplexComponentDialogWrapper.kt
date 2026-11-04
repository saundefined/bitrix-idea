package com.github.saundefined.bitrix_idea.dialogs

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.settings.AppSettingsState
import com.github.saundefined.bitrix_idea.templates.complex.ComplexComponentPage
import com.github.saundefined.bitrix_idea.templates.complex.ComplexComponentSpec
import com.github.saundefined.bitrix_idea.templates.complex.ComplexComponentWriter
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.psi.PsiDirectory
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.table.DefaultTableModel

class CreateComplexComponentDialogWrapper(
    private val project: Project,
    private val directory: PsiDirectory,
) : DialogWrapper(project, true) {
    private val vendorField = JBTextField()
    private val nameField = JBTextField()
    private val titleField = JBTextField()
    private val descriptionField = JBTextField()
    private val pagesModel = object : DefaultTableModel(
        arrayOf(
            message("create.complex.page.code"),
            message("create.complex.page.url"),
            message("create.complex.page.variables"),
        ),
        0,
    ) {
        override fun isCellEditable(row: Int, column: Int): Boolean = true
    }.apply {
        addRow(arrayOf("list", "", ""))
        addRow(arrayOf("detail", "#ELEMENT_ID#/", "ELEMENT_ID"))
    }
    private val pagesTable = JBTable(pagesModel)
    private val fallbackCheck = JBCheckBox(message("create.complex.fallback"))
    private val styleCheck = JBCheckBox(message("create.complex.style"), true)
    private val scriptCheck = JBCheckBox(message("create.complex.script"), true)
    private val resultModifierCheck = JBCheckBox(message("create.component.simple.result.modifier"))
    private val epilogCheck = JBCheckBox(message("create.component.simple.epilog"))
    private val ajaxCheck = JBCheckBox(message("create.complex.ajax"))

    private val contentPanel: JComponent = JTabbedPane().apply {
        preferredSize = Dimension(620, 420)
        addTab(message("create.complex.tab.main"), generalPanel())
        addTab(message("create.complex.tab.sef"), sefPanel())
        addTab(message("create.complex.tab.templates"), templatesPanel())
    }

    init {
        init()
        title = message("create.complex.title")
    }

    override fun createCenterPanel(): JComponent = contentPanel

    private fun generalPanel(): JComponent = FormBuilder.createFormBuilder()
        .addLabeledComponent(message("create.component.simple.vendor"), vendorField)
        .addLabeledComponent(message("create.component.simple.code"), nameField)
        .addLabeledComponent(message("create.component.simple.name"), titleField)
        .addLabeledComponent(message("create.component.simple.description"), descriptionField)
        .panel

    private fun sefPanel(): JComponent = JPanel(BorderLayout()).apply {
        add(pagesTable, BorderLayout.CENTER)
        add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JButton("+").apply {
                addActionListener { pagesModel.addRow(arrayOf("", "", "")) }
            })
            add(JButton("-").apply {
                addActionListener {
                    if (pagesTable.selectedRow >= 0) pagesModel.removeRow(pagesTable.selectedRow)
                }
            })
        }, BorderLayout.SOUTH)
    }

    private fun templatesPanel(): JComponent = JPanel().apply {
        layout = javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)
        add(fallbackCheck)
        add(styleCheck)
        add(scriptCheck)
        add(resultModifierCheck)
        add(epilogCheck)
        add(ajaxCheck)
    }

    override fun doValidate(): ValidationInfo? {
        if (!vendorField.text.matches(Regex("^[a-z][a-z0-9_]*$"))) {
            return ValidationInfo(message("create.component.simple.vendor.validation.fail"), vendorField)
        }
        if (!nameField.text.matches(Regex("^[a-z][a-z0-9_.-]*$"))) {
            return ValidationInfo(message("create.component.simple.code.validation.fail"), nameField)
        }
        if (titleField.text.isBlank()) {
            return ValidationInfo(message("create.component.simple.name.validation.fail"), titleField)
        }
        if (descriptionField.text.isBlank()) {
            return ValidationInfo(message("create.component.simple.description.validation.fail"), descriptionField)
        }
        val codes = (0 until pagesModel.rowCount).map { pagesModel.getValueAt(it, 0).toString().trim() }
        if (codes.any { it.isEmpty() } || codes.distinct().size != codes.size) {
            return ValidationInfo(message("create.complex.pages.validation"), pagesTable)
        }
        return null
    }

    override fun doOKAction() {
        val pages = (0 until pagesModel.rowCount).mapNotNull { row ->
            val code = pagesModel.getValueAt(row, 0).toString().trim()
            if (code.isEmpty()) return@mapNotNull null
            ComplexComponentPage(
                code = code,
                url = pagesModel.getValueAt(row, 1).toString().trim(),
                variables = pagesModel.getValueAt(row, 2).toString()
                    .split(',')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() },
            )
        }
        val spec = ComplexComponentSpec(
            vendor = vendorField.text.trim(),
            name = nameField.text.trim(),
            title = titleField.text.trim(),
            description = descriptionField.text.trim(),
            pages = pages,
            languages = AppSettingsState.getInstance().languages,
            fallbackTemplate = fallbackCheck.isSelected,
            style = styleCheck.isSelected,
            script = scriptCheck.isSelected,
            resultModifier = resultModifierCheck.isSelected,
            componentEpilog = epilogCheck.isSelected,
            ajax = ajaxCheck.isSelected,
        )

        ApplicationManager.getApplication().runWriteAction {
            ComplexComponentWriter.write(directory.virtualFile, spec)
        }
        super.doOKAction()
    }
}
