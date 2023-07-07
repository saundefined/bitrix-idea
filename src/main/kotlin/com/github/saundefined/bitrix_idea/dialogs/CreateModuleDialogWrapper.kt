package com.github.saundefined.bitrix_idea.dialogs

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.validation.ModuleCodeVerifier
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

class CreateModuleDialogWrapper : DialogWrapper(true) {
    private var codeField = JBTextField(53)
    private var nameField = JBTextField(53)
    private var descriptionField = JBTextField(53)
    private var vendorField = JBTextField(20)
    private var urlField = JBTextField(20)

    lateinit var view: IdeView
    lateinit var project: Project
    lateinit var directory: PsiDirectory

    init {
        init()
        title = message("create.module.form.title")
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val constraints = GridBagConstraints()

        constraints.anchor = GridBagConstraints.WEST
        constraints.insets = JBUI.insets(10)

        constraints.gridx = 0;
        constraints.gridy = 0;
        panel.add(JBLabel(message("create.module.code")), constraints);

        constraints.gridx = 1;
        constraints.gridwidth = 3;
        panel.add(codeField, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        panel.add(JBLabel(message("create.module.name")), constraints);

        constraints.gridx = 1;
        constraints.gridwidth = 3;
        panel.add(nameField, constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;
        panel.add(JBLabel(message("create.module.description")), constraints);

        constraints.gridx = 1;
        constraints.gridwidth = 3;
        panel.add(descriptionField, constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        panel.add(JBLabel(message("create.module.vendor")), constraints);

        constraints.gridx = 1;
        panel.add(vendorField, constraints);

        constraints.gridx = 2;
        panel.add(JBLabel(message("create.module.url")), constraints);

        constraints.gridx = 3;
        panel.add(urlField, constraints);

        return panel
    }

    override fun doValidate(): ValidationInfo? {
        if (ModuleCodeVerifier().verify(codeField.text).not()) {
            return ValidationInfo(message("create.module.code.validation.fail"), codeField)
        }
        if (nameField.text.isBlank()) {
            return ValidationInfo(message("create.module.name.validation.fail"), nameField)
        }
        if (descriptionField.text.isBlank()) {
            return ValidationInfo(message("create.module.description.validation.fail"), descriptionField)
        }
        if (vendorField.text.isBlank()) {
            return ValidationInfo(message("create.module.vendor.validation.fail"), vendorField)
        }
        if (UrlVerifier().verify(urlField.text).not()) {
            return ValidationInfo(message("create.module.url.validation.fail"), urlField)
        }

        return null
    }

    override fun doOKAction() {
        val validationInfo = doValidate()
        if (validationInfo == null) {
            val code = codeField.text
            val name = nameField.text
            val description = descriptionField.text
            val vendor = vendorField.text
            val url = urlField.text

            val templateManager = FileTemplateManager.getInstance(project)

            val properties: Properties = FileTemplateManager.getInstance(project).defaultProperties
            properties.setProperty("DEFAULT_MODULE_NAME", code)
            properties.setProperty("SNACK_CASED_MODULE_NAME", code.replace(".", "_"))
            properties.setProperty("UPPER_CASED_MODULE_NAME", code.replace(".", "_").uppercase(Locale.getDefault()))

            properties.setProperty("MODULE_NAME", name)
            properties.setProperty("MODULE_DESCRIPTION", description)
            properties.setProperty("PARTNER_NAME", vendor)
            properties.setProperty("PARTNER_URI", url)

            val moduleDirectory = directory.createSubdirectory(code)

            try {
                val rootFileNames =
                    listOf("prolog.php", "options.php", "include.php", "default_option.php", ".settings.php")

                rootFileNames.forEach { fileName ->
                    val template = templateManager.getJ2eeTemplate("Bitrix Module $fileName")
                    FileTemplateUtil.createFromTemplate(template, fileName, properties, moduleDirectory)
                }

                val installDirectory = moduleDirectory.createSubdirectory("install")

                val installFileNames = listOf("index.php", "version.php")
                installFileNames.forEach { fileName ->
                    val template = templateManager.getJ2eeTemplate("Bitrix Module install $fileName")
                    FileTemplateUtil.createFromTemplate(template, fileName, properties, installDirectory)
                }

                val languages = listOf("ru", "en")

                val langDirectory = moduleDirectory.createSubdirectory("lang")
                languages.forEach { language ->
                    val langInstallDirectory = langDirectory.createSubdirectory(language).createSubdirectory("install")
                    installFileNames
                        .filter { fileName -> fileName !== "version.php" }
                        .forEach { fileName ->
                            val template = templateManager.getJ2eeTemplate("Bitrix Module language install $fileName")
                            FileTemplateUtil.createFromTemplate(template, fileName, properties, langInstallDirectory)
                        }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            super.doOKAction()
        }
    }
}