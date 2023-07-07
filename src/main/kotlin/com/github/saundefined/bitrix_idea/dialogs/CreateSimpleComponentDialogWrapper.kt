package com.github.saundefined.bitrix_idea.dialogs

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.validation.ComponentSimpleCodeVerifier
import com.github.saundefined.bitrix_idea.validation.TemplateCodeVerifier
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

class CreateSimpleComponentDialogWrapper : DialogWrapper(true) {
    private var codeField = JBTextField(20)
    private var nameField = JBTextField(20)
    private var descriptionField = JBTextField(57)

    lateinit var view: IdeView
    lateinit var project: Project
    lateinit var directory: PsiDirectory

    init {
        init()
        title = message("create.component.simple.form.title")
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val constraints = GridBagConstraints()

        constraints.anchor = GridBagConstraints.WEST
        constraints.insets = JBUI.insets(10)

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        panel.add(JBLabel(message("create.component.simple.code")), constraints);

        constraints.gridx = 1;
        panel.add(codeField, constraints);

        constraints.gridx = 2;
        panel.add(JBLabel(message("create.component.simple.name")), constraints);

        constraints.gridx = 3;
        panel.add(nameField, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        panel.add(JBLabel(message("create.component.simple.description")), constraints);

        constraints.gridx = 1;
        constraints.gridwidth = 3;
        panel.add(descriptionField, constraints);

        return panel
    }

    override fun doValidate(): ValidationInfo? {
        if (ComponentSimpleCodeVerifier().verify(codeField.text).not()) {
            return ValidationInfo(message("create.component.simple.code.validation.fail"), codeField)
        }
        if (nameField.text.isBlank()) {
            return ValidationInfo(message("create.component.simple.name.validation.fail"), nameField)
        }
        if (descriptionField.text.isBlank()) {
            return ValidationInfo(message("create.component.simple.description.validation.fail"), descriptionField)
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

            properties.setProperty("UPPERCASE_COMPONENT_NAME", code.replace(".", "_").uppercase(Locale.getDefault()))
            properties.setProperty(
                "CAMEL_COMPONENT_NAME",
                code.replace(".", "_").split("_").joinToString("") { it ->
                    it.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(
                            Locale.getDefault()
                        ) else it.toString()
                    }
                }
            )
            properties.setProperty("COMPONENT_NAME", name)
            properties.setProperty("COMPONENT_DESCRIPTION", description)

            val componentDirectory = directory.createSubdirectory(code)

            try {
                val rootFileNames = listOf(".description.php", ".parameters.php", "class.php")
                rootFileNames.forEach { fileName ->
                    val template = templateManager.getJ2eeTemplate("Bitrix Simple Component $fileName")
                    FileTemplateUtil.createFromTemplate(template, fileName, properties, componentDirectory)
                }

                val templatesDirectory = componentDirectory.createSubdirectory("templates");
                val defaultTemplateDirectory = templatesDirectory.createSubdirectory(".default");

                val defaultTemplateDirectoryTemplate =
                    templateManager.getJ2eeTemplate("Bitrix Simple Component templates default template.php")
                FileTemplateUtil.createFromTemplate(
                    defaultTemplateDirectoryTemplate,
                    "template.php",
                    properties,
                    defaultTemplateDirectory
                )

                val languages = listOf("ru", "en")

                val langDirectory = componentDirectory.createSubdirectory("lang")
                languages.forEach { language ->
                    val langInstallDirectory = langDirectory.createSubdirectory(language)
                    rootFileNames
                        .filter { fileName -> fileName !== "class.php" }
                        .forEach { fileName ->
                            val template = templateManager.getJ2eeTemplate("Bitrix Simple Component language $fileName")
                            FileTemplateUtil.createFromTemplate(template, fileName, properties, langInstallDirectory)
                        }

                    val languageTemplatesDirectory =
                        langInstallDirectory.createSubdirectory("templates");
                    val languageDefaultTemplateDirectory =
                        languageTemplatesDirectory.createSubdirectory(".default");
                    
                    val languageDefaultTemplateDirectoryTemplate =
                        templateManager.getJ2eeTemplate("Bitrix Simple Component language templates default template.php")
                    FileTemplateUtil.createFromTemplate(
                        languageDefaultTemplateDirectoryTemplate,
                        "template.php",
                        properties,
                        languageDefaultTemplateDirectory
                    )
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }

            super.doOKAction()
        }
    }
}