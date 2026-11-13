package com.github.saundefined.bitrix_idea.dialogs

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.settings.AppSettingsState
import com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
import com.github.saundefined.bitrix_idea.templates.component.ComponentClassGenerator
import com.github.saundefined.bitrix_idea.templates.component.ComponentParameterSpec
import com.github.saundefined.bitrix_idea.templates.component.ComponentParametersWriter
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

/**
 * Извлекает выделенный код в новый компонент и заменяет его IncludeComponent.
 */
class ExtractComponentDialogWrapper(
    private val project: Project,
    private val editor: Editor,
    private val file: PsiFile,
) : DialogWrapper(project, true) {
    private var vendor: String = AppSettingsState.getInstance().vendorCode.lowercase().ifBlank { "vendor" }
    private var code: String = "extracted"
    private var displayName: String = "Извлечённый компонент"
    private var description: String = ""
    private var templateName: String = ".default"

    private val selectedText: String = editor.selectionModel.selectedText.orEmpty()

    private val contentPanel = panel {
        row(message("extract.vendor")) {
            textField()
                .bindText(::vendor)
                .focused()
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row(message("extract.code")) {
            textField()
                .bindText(::code)
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row(message("extract.name")) {
            textField()
                .bindText(::displayName)
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row(message("extract.description")) {
            textField()
                .bindText(::description)
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row(message("extract.template")) {
            textField()
                .bindText(::templateName)
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row {
            comment(message("extract.hint"))
        }
    }

    init {
        init()
        title = message("extract.title")
    }

    override fun createCenterPanel(): JComponent = contentPanel

    override fun doValidate(): ValidationInfo? {
        contentPanel.apply()
        if (!vendor.matches(Regex("^[a-z][a-z0-9_.-]*$"))) return ValidationInfo(message("extract.vendor.validation"), null)
        if (!code.matches(Regex("^[a-z][a-z0-9_.-]*$"))) return ValidationInfo(message("extract.code.validation"), null)
        if (selectedText.isBlank()) return ValidationInfo(message("extract.selection.validation"), null)
        return null
    }

    override fun doOKAction() {
        val root = BitrixProjectStructure.bitrixRoot(project)
        if (root == null) {
            Messages.showErrorDialog(project, message("extract.no.root"), title)
            return
        }
        val componentPath = "$vendor/$code"
        val target = root.findChild("local")?.findChild("components")?.findFileByRelativePath(componentPath)
        if (target != null) {
            Messages.showErrorDialog(project, message("extract.exists"), title)
            return
        }

        val parameters = Regex("""\${'$'}arParams\s*\[\s*['"]([^'"]+)['"]""")
            .findAll(selectedText)
            .map { ComponentParameterSpec(it.groupValues[1], it.groupValues[1]) }
            .distinctBy { it.key }
            .toList()
        val className = ComponentClassGenerator.classNameFor(code)
        val template = templateName.trim().ifBlank { ".default" }

        WriteCommandAction.runWriteCommandAction(project) {
            var parent = root.findChild("local") ?: root.createChildDirectory(this, "local")
            parent = parent.findChild("components") ?: parent.createChildDirectory(this, "components")
            parent = parent.findChild(vendor) ?: parent.createChildDirectory(this, vendor)
            val component = parent.createChildDirectory(this, code)
            VfsUtil.saveText(
                component.createChildData(this, ".description.php"),
                "<?php\n\n\$arComponentDescription = [\n    'NAME' => '${escape(displayName)}',\n    'DESCRIPTION' => '${escape(description)}',\n];\n",
            )
            VfsUtil.saveText(component.createChildData(this, "class.php"), ComponentClassGenerator.generate(className))
            ComponentParametersWriter.write(component, parameters, AppSettingsState.getInstance().languages)
            val templates = component.createChildDirectory(this, "templates")
            val templateDir = templates.createChildDirectory(this, template)
            VfsUtil.saveText(
                templateDir.createChildData(this, "template.php"),
                if (selectedText.trimStart().startsWith("<?php")) selectedText else "<?php\n\n$selectedText",
            )

            val document = editor.document
            val start = editor.selectionModel.selectionStart
            val end = editor.selectionModel.selectionEnd
            val call = "\$APPLICATION->IncludeComponent(\n    '$vendor:$code',\n    '${escape(template)}',\n    [],\n    false\n);"
            document.replaceString(start, end, call)
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }
        super.doOKAction()
    }

    private fun escape(value: String): String = value.replace("\\", "\\\\").replace("'", "\\'")
}
