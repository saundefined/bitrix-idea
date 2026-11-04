package com.github.saundefined.bitrix_idea.dialogs

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.structure.BitrixModuleNames
import com.github.saundefined.bitrix_idea.templates.BitrixTemplateNames
import com.github.saundefined.bitrix_idea.templates.BitrixTemplateWriter
import com.github.saundefined.bitrix_idea.templates.simple_component.SimpleComponentSpec
import com.github.saundefined.bitrix_idea.templates.simple_component.SimpleComponentWriter
import com.github.saundefined.bitrix_idea.ui.LocalizationTable
import com.github.saundefined.bitrix_idea.validation.ComponentSimpleCodeVerifier
import com.github.saundefined.bitrix_idea.validation.ComponentVendorVerifier
import com.github.saundefined.bitrix_idea.validation.ModuleCodeVerifier
import com.github.saundefined.bitrix_idea.validation.UrlVerifier
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
import java.util.Locale
import java.util.Properties
import javax.swing.JComponent

class CreateModuleDialogWrapper(
    project: Project,
    directory: PsiDirectory,
) : AbstractCreateDialogWrapper(project, directory) {
    private lateinit var localizationTable: LocalizationTable

    private var vendor: String = ""
    private var url: String = ""

    private var installSteps: Boolean = true
    private var sqlFiles: Boolean = true
    private var adminMenu: Boolean = true
    private var generateOptions: Boolean = true
    private var settingsControllers: Boolean = true
    private var settingsServices: Boolean = true
    private var settingsEvents: Boolean = true
    private var settingsConsole: Boolean = true
    private var createComponent: Boolean = false
    private var componentVendor: String = ""
    private var componentCode: String = ""
    private var componentName: String = ""
    private var componentDescription: String = ""
    private var componentResultModifier: Boolean = false
    private var componentEpilog: Boolean = false

    init {
        initialize("create.module.form.title")
    }

    override fun createCenterPanel(): JComponent {
        localizationTable = LocalizationTable(settings.languages) { pack() }

        return panel {
            row {
                label(message("create.module.code"))
                textField()
                    .bindText(::code)
                    .focused()
                    .text(settings.vendorCode.lowercase().let { if (it.isBlank()) "" else "$it." })
                    .align(AlignX.FILL)
                    .validationOnApply {
                        if (!ModuleCodeVerifier.verify(it.text)) {
                            error(message("create.module.code.validation.fail"))
                        } else {
                            null
                        }
                    }

                label(message("create.module.name"))
                textField()
                    .bindText(::name)
                    .align(AlignX.FILL)
                    .validationOnApply {
                        if (it.text.isNullOrBlank()) {
                            error(message("create.module.name.validation.fail"))
                        } else {
                            null
                        }
                    }
            }.layout(RowLayout.PARENT_GRID)

            row(message("create.module.description")) {
                textField()
                    .bindText(::description)
                    .align(AlignX.FILL)
                    .validationOnApply {
                        if (it.text.isNullOrBlank()) {
                            error(message("create.module.description.validation.fail"))
                        } else {
                            null
                        }
                    }
            }.layout(RowLayout.PARENT_GRID)

            row {
                label(message("create.module.vendor"))
                textField()
                    .bindText(::vendor)
                    .text(settings.vendorName)
                    .align(AlignX.FILL)
                    .validationOnApply {
                        if (it.text.isNullOrBlank()) {
                            error(message("create.module.vendor.validation.fail"))
                        } else {
                            null
                        }
                    }

                label(message("create.module.url"))
                textField()
                    .bindText(::url)
                    .text(settings.vendorWebsite)
                    .align(AlignX.FILL)
                    .validationOnApply {
                        if (!UrlVerifier.verify(it.text)) {
                            error(message("create.module.url.validation.fail"))
                        } else {
                            null
                        }
                    }
            }.layout(RowLayout.PARENT_GRID)

            row {
                label(message("localization.table.row.name"))
                    .align(AlignY.TOP)
                cell(localizationTable.component)
                    .align(AlignX.FILL)
            }.layout(RowLayout.PARENT_GRID)

            collapsibleGroup(message("create.module.extra.group"), false) {
                row {
                    checkBox(message("create.module.extra.install.steps"))
                        .bindSelected(::installSteps)
                }
                row {
                    checkBox(message("create.module.extra.sql"))
                        .bindSelected(::sqlFiles)
                }
                row {
                    checkBox(message("create.module.extra.admin"))
                        .bindSelected(::adminMenu)
                }
                row {
                    checkBox(message("create.module.extra.options"))
                        .bindSelected(::generateOptions)
                }
                row {
                    checkBox(message("create.module.extra.settings.controllers"))
                        .bindSelected(::settingsControllers)
                }
                row {
                    checkBox(message("create.module.extra.settings.services"))
                        .bindSelected(::settingsServices)
                }
                row {
                    checkBox(message("create.module.extra.settings.events"))
                        .bindSelected(::settingsEvents)
                }
                row {
                    checkBox(message("create.module.extra.settings.console"))
                        .bindSelected(::settingsConsole)
                }
            }

            collapsibleGroup(message("create.module.component.group"), false) {
                row {
                    checkBox(message("create.module.component.create"))
                        .bindSelected(::createComponent)
                }
                row {
                    label(message("create.component.simple.vendor"))
                    textField()
                        .bindText(::componentVendor)
                        .text(settings.vendorCode.lowercase())
                        .align(AlignX.FILL)
                        .validationOnApply {
                            if (createComponent && !ComponentVendorVerifier.verify(it.text)) {
                                error(message("create.component.simple.vendor.validation.fail"))
                            } else {
                                null
                            }
                        }

                    label(message("create.component.simple.code"))
                    textField()
                        .bindText(::componentCode)
                        .align(AlignX.FILL)
                        .validationOnApply {
                            if (createComponent && !ComponentSimpleCodeVerifier.verify(it.text)) {
                                error(message("create.component.simple.code.validation.fail"))
                            } else {
                                null
                            }
                        }
                }.layout(RowLayout.PARENT_GRID)

                row {
                    label(message("create.component.simple.name"))
                    textField()
                        .bindText(::componentName)
                        .align(AlignX.FILL)
                        .validationOnApply {
                            if (createComponent && it.text.isNullOrBlank()) {
                                error(message("create.component.simple.name.validation.fail"))
                            } else {
                                null
                            }
                        }
                }.layout(RowLayout.PARENT_GRID)

                row(message("create.component.simple.description")) {
                    textField()
                        .bindText(::componentDescription)
                        .align(AlignX.FILL)
                        .validationOnApply {
                            if (createComponent && it.text.isNullOrBlank()) {
                                error(message("create.component.simple.description.validation.fail"))
                            } else {
                                null
                            }
                        }
                }.layout(RowLayout.PARENT_GRID)

                row {
                    checkBox(message("create.component.simple.result.modifier"))
                        .bindSelected(::componentResultModifier)
                    checkBox(message("create.component.simple.epilog"))
                        .bindSelected(::componentEpilog)
                }
            }
        }
    }

    override fun createFiles(templateManager: FileTemplateManager, properties: Properties) {
        properties.setProperty("DEFAULT_MODULE_NAME", code)
        properties.setProperty("SNACK_CASED_MODULE_NAME", code.replace(".", "_"))
        properties.setProperty("UPPER_CASED_MODULE_NAME", code.replace(".", "_").uppercase(Locale.getDefault()))

        properties.setProperty("MODULE_NAME", escapePhp(name))
        properties.setProperty("MODULE_DESCRIPTION", escapePhp(description))
        properties.setProperty("PARTNER_NAME", escapePhp(vendor))
        properties.setProperty("PARTNER_URI", escapePhp(url))
        properties.setProperty("SETTINGS_SECTIONS", settingsSections())

        val componentDirectory = if (createComponent) {
            "${componentVendor.trim()}/${componentCode.trim()}"
        } else {
            ""
        }
        properties.setProperty(
            "MODULE_INSTALL_FILES",
            if (createComponent) {
                "        CopyDirFiles(__DIR__.'/components', \$_SERVER['DOCUMENT_ROOT'].'/local/components', true, true);"
            } else {
                ""
            },
        )
        properties.setProperty(
            "MODULE_UNINSTALL_FILES",
            if (createComponent) {
                "        Directory::deleteDirectory(Application::getDocumentRoot().'/local/components/$componentDirectory');"
            } else {
                ""
            },
        )
        properties.setProperty(
            "MODULE_INSTALL_RESULT_CALL",
            if (installSteps) {
                """
                |        ${'$'}APPLICATION->IncludeAdminFile(
                |            'Установка модуля '.${'$'}this->MODULE_NAME,
                |            __DIR__.'/step.php'
                |        );
                |""".trimMargin()
            } else {
                ""
            },
        )
        properties.setProperty(
            "MODULE_UNINSTALL_RESULT_CALL",
            if (installSteps) {
                """
                |        ${'$'}APPLICATION->IncludeAdminFile(
                |            'Удаление модуля '.${'$'}this->MODULE_NAME,
                |            __DIR__.'/unstep.php'
                |        );
                |""".trimMargin()
            } else {
                ""
            },
        )

        val moduleDirectory = directory.createSubdirectory(code)
        val moduleFiles = mutableListOf("prolog.php", "include.php", "default_option.php", ".settings.php")
        if (generateOptions) {
            moduleFiles += "options.php"
        }
        BitrixTemplateWriter.createFiles(
            templateManager,
            moduleDirectory,
            BitrixTemplateNames.MODULE,
            moduleFiles,
            properties,
        )

        val installDirectory = moduleDirectory.createSubdirectory("install")
        if (createComponent) {
            SimpleComponentWriter.write(
                templateManager = templateManager,
                baseDirectory = installDirectory.createSubdirectory("components"),
                spec = SimpleComponentSpec(
                    vendor = componentVendor.trim(),
                    name = componentCode.trim(),
                    displayName = componentName,
                    description = componentDescription,
                    vendorName = settings.vendorName.ifBlank { componentVendor.trim() },
                    languages = localizationTable.languages,
                    resultModifier = componentResultModifier,
                    componentEpilog = componentEpilog,
                ),
                properties = properties,
            )
        }

        BitrixTemplateWriter.createFiles(
            templateManager,
            installDirectory,
            BitrixTemplateNames.MODULE_INSTALL,
            listOf("index.php", "version.php"),
            properties,
        )

        if (installSteps) {
            BitrixTemplateWriter.createFiles(
                templateManager,
                installDirectory,
                BitrixTemplateNames.MODULE_INSTALL,
                listOf("step.php", "unstep.php"),
                properties,
            )
        }

        if (sqlFiles) {
            val sqlDirectory = installDirectory.createSubdirectory("db").createSubdirectory("mysql")
            BitrixTemplateWriter.createFiles(
                templateManager,
                sqlDirectory,
                BitrixTemplateNames.MODULE_INSTALL_DB,
                listOf("install.sql", "uninstall.sql"),
                properties,
            )
        }

        if (adminMenu) {
            val adminDirectory = moduleDirectory.createSubdirectory("admin")
            BitrixTemplateWriter.createFile(
                templateManager,
                adminDirectory,
                BitrixTemplateNames.MODULE_ADMIN_MENU,
                "menu.php",
                properties,
            )
        }

        val langDirectory = moduleDirectory.createSubdirectory("lang")
        localizationTable.languages.forEach { language ->
            val languageDirectory = langDirectory.createSubdirectory(language)
            val langInstallDirectory = languageDirectory.createSubdirectory("install")
            BitrixTemplateWriter.createFiles(
                templateManager,
                langInstallDirectory,
                BitrixTemplateNames.MODULE_LANGUAGE_INSTALL,
                listOf("index.php"),
                properties,
            )
            if (installSteps) {
                BitrixTemplateWriter.createFiles(
                    templateManager,
                    langInstallDirectory,
                    BitrixTemplateNames.MODULE_LANGUAGE_INSTALL,
                    listOf("step.php", "unstep.php"),
                    properties,
                )
            }
            if (adminMenu) {
                val langAdminDirectory = languageDirectory.createSubdirectory("admin")
                BitrixTemplateWriter.createFile(
                    templateManager,
                    langAdminDirectory,
                    BitrixTemplateNames.MODULE_LANGUAGE_ADMIN_MENU,
                    "menu.php",
                    properties,
                )
            }
        }
    }

    private fun escapePhp(value: String): String = value.replace("\\", "\\\\").replace("'", "\\'")

    private fun settingsSections(): String {
        val namespace = BitrixModuleNames.namespaceFor(code)
        val controllerNamespace = "\\\\" + namespace.replace("\\", "\\\\") + "\\\\Controller"
        val sections = mutableListOf<String>()

        if (settingsControllers) {
            sections += """
                |    'controllers' => [
                |        'value' => [
                |            'defaultNamespace' => '$controllerNamespace',
                |        ],
                |        'readonly' => true,
                |    ],""".trimMargin()
        }
        if (settingsServices) {
            sections += """
                |    'services' => [
                |        'value' => [],
                |    ],""".trimMargin()
        }
        if (settingsEvents) {
            sections += """
                |    'events' => [
                |        'value' => [],
                |    ],""".trimMargin()
        }
        if (settingsConsole) {
            sections += """
                |    'console' => [
                |        'value' => [],
                |    ],""".trimMargin()
        }

        return sections.joinToString("\n")
    }
}
