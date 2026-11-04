package com.github.saundefined.bitrix_idea.dialogs

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.templates.simple_component.SimpleComponentSpec
import com.github.saundefined.bitrix_idea.templates.simple_component.SimpleComponentWriter
import com.github.saundefined.bitrix_idea.ui.LocalizationTable
import com.github.saundefined.bitrix_idea.validation.ComponentSimpleCodeVerifier
import com.github.saundefined.bitrix_idea.validation.ComponentVendorVerifier
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.text
import java.util.Properties
import javax.swing.JComponent

class CreateSimpleComponentDialogWrapper(
    project: Project,
    directory: PsiDirectory,
) : AbstractCreateDialogWrapper(project, directory) {
    private lateinit var localizationTable: LocalizationTable

    private var vendor: String = ""
    private var resultModifier: Boolean = false
    private var componentEpilog: Boolean = false
    private var complex: Boolean = false

    init {
        initialize("create.component.simple.form.title")
    }

    override fun createCenterPanel(): JComponent {
        localizationTable = LocalizationTable(settings.languages) { pack() }

        return panel {
            row {
                label(message("create.component.simple.vendor"))
                textField()
                    .bindText(::vendor)
                    .text(settings.vendorCode.lowercase())
                    .focused()
                    .align(AlignX.FILL)
                    .validationOnApply {
                        if (!ComponentVendorVerifier.verify(it.text)) {
                            error(message("create.component.simple.vendor.validation.fail"))
                        } else {
                            null
                        }
                    }

                label(message("create.component.simple.code"))
                textField()
                    .bindText(::code)
                    .align(AlignX.FILL)
                    .validationOnApply {
                        if (!ComponentSimpleCodeVerifier.verify(it.text)) {
                            error(message("create.component.simple.code.validation.fail"))
                        } else {
                            null
                        }
                    }
            }.layout(RowLayout.PARENT_GRID)

            row {
                label(message("create.component.simple.name"))
                textField()
                    .bindText(::name)
                    .align(AlignX.FILL)
                    .validationOnApply {
                        if (it.text.isNullOrBlank()) {
                            error(message("create.component.simple.name.validation.fail"))
                        } else {
                            null
                        }
                    }
            }.layout(RowLayout.PARENT_GRID)

            row(message("create.component.simple.description")) {
                textField()
                    .bindText(::description)
                    .align(AlignX.FILL)
                    .validationOnApply {
                        if (it.text.isNullOrBlank()) {
                            error(message("create.component.simple.description.validation.fail"))
                        } else {
                            null
                        }
                    }
            }.layout(RowLayout.PARENT_GRID)

            row {
                checkBox(message("create.component.simple.result.modifier"))
                    .bindSelected(::resultModifier)
                checkBox(message("create.component.simple.epilog"))
                    .bindSelected(::componentEpilog)
            }

            row {
                checkBox(message("create.component.simple.complex"))
                    .bindSelected(::complex)
            }

            row {
                label(message("localization.table.row.name"))
                    .align(AlignY.TOP)
                cell(localizationTable.component)
                    .align(AlignX.FILL)
            }.layout(RowLayout.PARENT_GRID)
        }
    }

    override fun createFiles(templateManager: FileTemplateManager, properties: Properties) {
        SimpleComponentWriter.write(
            templateManager = templateManager,
            baseDirectory = directory,
            spec = SimpleComponentSpec(
                vendor = vendor.trim(),
                name = code.trim(),
                displayName = name,
                description = description,
                vendorName = settings.vendorName.ifBlank { vendor.trim() },
                languages = localizationTable.languages,
                resultModifier = resultModifier,
                componentEpilog = componentEpilog,
                complex = complex,
            ),
            properties = properties,
        )
    }
}
