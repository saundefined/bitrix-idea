package com.github.saundefined.bitrix_idea.dialogs

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.validation.TemplateCodeVerifier
import com.intellij.ide.IdeView
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiDirectory
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import java.util.*
import javax.swing.JComponent

class CreateTemplateDialogWrapper : DialogWrapper(true) {
    var code: String = ""
    var name: String = ""
    var description: String = ""

    lateinit var view: IdeView
    lateinit var project: Project
    lateinit var directory: PsiDirectory

    init {
        init()
        title = message("create.template.form.title")
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row {
                label(message("create.template.code"))
                textField()
                    .bindText(::code)
                    .focused()
                    .horizontalAlign(HorizontalAlign.FILL)
                    .validationOnApply {
                        val value = it.text
                        when {
                            TemplateCodeVerifier().verify(value)
                                .not() -> error(message("create.template.code.validation.fail"))

                            else -> null
                        }
                    }

                label(message("create.template.name"))
                textField()
                    .bindText(::name)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .validationOnApply {
                        val value = it.text
                        when {
                            value === null -> error(message("create.template.name.validation.fail"))
                            else -> null
                        }
                    }
            }.layout(RowLayout.PARENT_GRID)

            row(message("create.template.description")) {
                textField()
                    .bindText(::description)
                    .horizontalAlign(HorizontalAlign.FILL)
            }.layout(RowLayout.PARENT_GRID)
        }
    }

    override fun doOKAction() {
        super.applyFields()

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