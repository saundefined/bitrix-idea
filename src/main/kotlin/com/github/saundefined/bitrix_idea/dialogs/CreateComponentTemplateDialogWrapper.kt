package com.github.saundefined.bitrix_idea.dialogs

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.settings.AppSettingsState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiDirectory
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.Dimension
import javax.swing.JComponent

class CreateComponentTemplateDialogWrapper(
    private val project: Project,
    private val directory: PsiDirectory,
) : DialogWrapper(project, true) {
    private val nameField = JBTextField()
    private val copyDefaultCheck = JBCheckBox(message("create.component.template.copy.default"), true)
    private val resultModifierCheck = JBCheckBox(message("create.component.simple.result.modifier"))
    private val epilogCheck = JBCheckBox(message("create.component.simple.epilog"))
    private val templateLangCheck = JBCheckBox(message("create.component.template.lang"), true)

    private val contentPanel = FormBuilder.createFormBuilder()
        .addLabeledComponent(message("create.component.template.name"), nameField)
        .addComponent(copyDefaultCheck)
        .addComponent(resultModifierCheck)
        .addComponent(epilogCheck)
        .addComponent(templateLangCheck)
        .panel
        .apply { preferredSize = Dimension(480, 140) }

    init {
        init()
        title = message("create.component.template.title")
    }

    override fun createCenterPanel(): JComponent = contentPanel

    override fun doValidate(): ValidationInfo? {
        val name = nameField.text.trim()
        if (!name.matches(Regex("^[a-zA-Z0-9_.-]+$"))) {
            return ValidationInfo(message("create.component.template.name.validation"), nameField)
        }
        return null
    }

    override fun doOKAction() {
        val name = nameField.text.trim()
        val templates = directory.virtualFile.findChild("templates")
        if (templates?.findChild(name) != null) {
            Messages.showErrorDialog(
                project,
                message("create.component.template.exists"),
                message("create.component.template.title"),
            )
            return
        }

        ApplicationManager.getApplication().runWriteAction {
            val templatesDir = templates ?: directory.virtualFile.createChildDirectory(this, "templates")
            val target = templatesDir.createChildDirectory(this, name)
            val defaultDir = templatesDir.findChild(".default")
            if (copyDefaultCheck.isSelected && defaultDir != null) {
                VfsUtil.copyDirectory(this, defaultDir, target, null)
            } else {
                VfsUtil.saveText(
                    target.createChildData(this, "template.php"),
                    "<?php\n\nif (!defined('B_PROLOG_INCLUDED') || B_PROLOG_INCLUDED !== true) {\n    die();\n}\n",
                )
                VfsUtil.saveText(target.createChildData(this, "style.css"), "")
                VfsUtil.saveText(target.createChildData(this, "script.js"), "")
            }

            if (resultModifierCheck.isSelected && target.findChild("result_modifier.php") == null) {
                VfsUtil.saveText(
                    target.createChildData(this, "result_modifier.php"),
                    "<?php\n\nif (!defined('B_PROLOG_INCLUDED') || B_PROLOG_INCLUDED !== true) {\n    die();\n}\n",
                )
            }
            if (epilogCheck.isSelected && target.findChild("component_epilog.php") == null) {
                VfsUtil.saveText(
                    target.createChildData(this, "component_epilog.php"),
                    "<?php\n\nuse Bitrix\\Main\\Localization\\Loc;\n\nif (!defined('B_PROLOG_INCLUDED') || B_PROLOG_INCLUDED !== true) {\n    die();\n}\n\nLoc::loadMessages(__FILE__);\n",
                )
            }
            if (templateLangCheck.isSelected) {
                val langRoot = target.findChild("lang") ?: target.createChildDirectory(this, "lang")
                AppSettingsState.getInstance().languages.forEach { language ->
                    val langDir = langRoot.findChild(language) ?: langRoot.createChildDirectory(this, language)
                    if (langDir.findChild("template.php") == null) {
                        VfsUtil.saveText(
                            langDir.createChildData(this, "template.php"),
                            "<?php\n\n\$MESS['TEMPLATE_TITLE'] = '${name}';\n",
                        )
                    }
                }
            }
        }
        super.doOKAction()
    }
}
