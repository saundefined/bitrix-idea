package com.github.saundefined.bitrix_idea.dialogs

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.validation.ModuleCodeVerifier
import com.github.saundefined.bitrix_idea.validation.TemplateCodeVerifier
import com.github.saundefined.bitrix_idea.validation.UrlVerifier
import com.intellij.ide.IdeView
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.psi.PsiDirectory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.util.*
import javax.swing.JComponent
import javax.swing.JPanel

class CreateTemplateDialogWrapper : DialogWrapper(true) {
    private var codeField = JBTextField(20)
    private var nameField = JBTextField(20)
    private var descriptionField = JBTextField(55)

    lateinit var view: IdeView
    lateinit var project: Project
    lateinit var directory: PsiDirectory

    init {
        init()
        title = message("create.template.form.title")
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val constraints = GridBagConstraints()

        constraints.anchor = GridBagConstraints.WEST
        constraints.insets = JBUI.insets(10)

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        panel.add(JBLabel(message("create.template.code")), constraints);

        constraints.gridx = 1;
        panel.add(codeField, constraints);

        constraints.gridx = 2;
        panel.add(JBLabel(message("create.template.name")), constraints);

        constraints.gridx = 3;
        panel.add(nameField, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        panel.add(JBLabel(message("create.template.description")), constraints);

        constraints.gridx = 1;
        constraints.gridwidth = 3;
        panel.add(descriptionField, constraints);

        return panel
    }

    override fun doValidate(): ValidationInfo? {
        if (TemplateCodeVerifier().verify(codeField.text).not()) {
            return ValidationInfo(message("create.template.code.validation.fail"), codeField)
        }
        if (nameField.text.isBlank()) {
            return ValidationInfo(message("create.template.name.validation.fail"), nameField)
        }

        return null
    }

    override fun doOKAction() {
        val validationInfo = doValidate()
        if (validationInfo == null) {
            val code = codeField.text
            val name = nameField.text
            val description = descriptionField.text

            val templateManager = FileTemplateManager.getInstance(project)

            val properties: Properties = FileTemplateManager.getInstance(project).defaultProperties
            properties.setProperty("TEMPLATE_NAME", name)
            properties.setProperty("TEMPLATE_DESCRIPTION", description)

            val templateDirectory = directory.createSubdirectory(code)

            try {
                val fileNames = listOf(
                    ".styles.php", "description.php", "footer.php", "header.php", "styles.css", "template_styles.css"
                )

                fileNames.forEach { fileName ->
                    val template = templateManager.getJ2eeTemplate("Bitrix Template $fileName")
                    FileTemplateUtil.createFromTemplate(template, fileName, properties, templateDirectory)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            super.doOKAction()
        }
    }
}