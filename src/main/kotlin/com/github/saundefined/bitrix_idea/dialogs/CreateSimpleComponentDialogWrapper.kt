package com.github.saundefined.bitrix_idea.dialogs

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.validation.ComponentSimpleCodeVerifier
import com.intellij.ide.IdeView
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiDirectory
import com.intellij.ui.UIBundle
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import java.util.*
import javax.swing.JComponent

class CreateSimpleComponentDialogWrapper : DialogWrapper(true) {
    var code: String = ""
    var name: String = ""
    var description: String = ""

    lateinit var view: IdeView
    lateinit var project: Project
    lateinit var directory: PsiDirectory

    init {
        init()
        title = message("create.component.simple.form.title")
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row {
                label(message("create.component.simple.code"))
                textField()
                    .bindText(::code)
                    .focused()
                    .horizontalAlign(HorizontalAlign.FILL)
                    .validationOnApply {
                        val value = it.text
                        when {
                            ComponentSimpleCodeVerifier().verify(value)
                                .not() -> error(message("create.component.simple.code.validation.fail"))

                            else -> null
                        }
                    }

                label(message("create.component.simple.name"))
                textField()
                    .bindText(::name)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .validationOnApply {
                        val value = it.text
                        when {
                            value === null -> error(UIBundle.message("create.component.simple.name.validation.fail"))
                            else -> null
                        }
                    }

            }.layout(RowLayout.PARENT_GRID)

            row(message("create.component.simple.description")) {
                textField()
                    .bindText(::description)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .validationOnApply {
                        val value = it.text
                        when {
                            value === null -> error(message("create.component.simple.description.validation.fail"))
                            else -> null
                        }
                    }
            }.layout(RowLayout.PARENT_GRID)
        }
    }

    override fun doOKAction() {
        super.applyFields()

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