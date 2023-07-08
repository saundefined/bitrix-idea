package com.github.saundefined.bitrix_idea.dialogs

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.validation.ModuleCodeVerifier
import com.github.saundefined.bitrix_idea.validation.UrlVerifier
import com.intellij.ide.IdeView
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiDirectory
import javax.swing.JComponent
import com.intellij.ui.UIBundle
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import java.util.*

class CreateModuleDialogWrapper : DialogWrapper(true) {
    var code: String = ""
    var name: String = ""
    var description: String = ""
    var vendor: String = ""
    var url: String = ""

    lateinit var view: IdeView
    lateinit var project: Project
    lateinit var directory: PsiDirectory

    init {
        init()
        title = message("create.module.form.title")
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row {
                label(message("create.module.code"))
                textField()
                    .bindText(::code)
                    .focused()
                    .horizontalAlign(HorizontalAlign.FILL)
                    .validationOnApply {
                        val value = it.text
                        when {
                            ModuleCodeVerifier().verify(value)
                                .not() -> error(message("create.module.code.validation.fail"))

                            else -> null
                        }
                    }
                
                label(message("create.module.name"))
                textField()
                    .bindText(::name)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .validationOnApply {
                        val value = it.text
                        when {
                            value === null -> error(UIBundle.message("create.module.name.validation.fail"))
                            else -> null
                        }
                    }
            }.layout(RowLayout.PARENT_GRID)
            
            row(message("create.module.description")) {
                textField()
                    .bindText(::description)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .validationOnApply {
                        val value = it.text
                        when {
                            value === null -> error(message("create.module.description.validation.fail"))
                            else -> null
                        }
                    }
            }.layout(RowLayout.PARENT_GRID)

            row {
                label(message("create.module.vendor"))
                textField()
                    .bindText(::vendor)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .validationOnApply {
                        val value = it.text
                        when {
                            value === null -> error(message("create.module.vendor.validation.fail"))
                            else -> null
                        }
                    }

                label(message("create.module.url"))
                textField()
                    .bindText(::url)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .validationOnApply {
                        val value = it.text
                        when {
                            UrlVerifier().verify(value)
                                .not() -> error(message("create.module.url.validation.fail"))

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
                installFileNames.filter { fileName -> fileName !== "version.php" }.forEach { fileName ->
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