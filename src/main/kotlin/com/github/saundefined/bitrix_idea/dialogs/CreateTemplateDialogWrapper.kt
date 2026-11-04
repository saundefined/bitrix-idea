package com.github.saundefined.bitrix_idea.dialogs

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.templates.BitrixTemplateNames
import com.github.saundefined.bitrix_idea.templates.BitrixTemplateWriter
import com.github.saundefined.bitrix_idea.validation.TemplateCodeVerifier
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.text
import java.util.Properties
import javax.swing.JComponent

class CreateTemplateDialogWrapper(
    project: Project,
    directory: PsiDirectory,
) : AbstractCreateDialogWrapper(project, directory) {
    init {
        initialize("create.template.form.title")
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row {
                label(message("create.template.code"))
                textField()
                    .bindText(::code)
                    .focused()
                    .align(AlignX.FILL)
                    .validationOnApply {
                        if (!TemplateCodeVerifier.verify(it.text)) {
                            error(message("create.template.code.validation.fail"))
                        } else {
                            null
                        }
                    }

                label(message("create.template.name"))
                textField()
                    .bindText(::name)
                    .align(AlignX.FILL)
                    .validationOnApply {
                        if (it.text.isNullOrBlank()) {
                            error(message("create.template.name.validation.fail"))
                        } else {
                            null
                        }
                    }
            }.layout(RowLayout.PARENT_GRID)

            row(message("create.template.description")) {
                textField()
                    .bindText(::description)
                    .align(AlignX.FILL)
            }.layout(RowLayout.PARENT_GRID)
        }
    }

    override fun createFiles(templateManager: FileTemplateManager, properties: Properties) {
        properties.setProperty("TEMPLATE_NAME", name)
        properties.setProperty("TEMPLATE_DESCRIPTION", description)

        val templateDirectory = directory.createSubdirectory(code)
        BitrixTemplateWriter.createFiles(
            templateManager,
            templateDirectory,
            BitrixTemplateNames.TEMPLATE,
            listOf(".styles.php", "description.php", "footer.php", "header.php", "styles.css", "template_styles.css"),
            properties,
        )
    }
}
