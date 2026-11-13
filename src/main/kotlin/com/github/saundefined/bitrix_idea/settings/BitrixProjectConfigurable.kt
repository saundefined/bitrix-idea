package com.github.saundefined.bitrix_idea.settings

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.library.BitrixLibraryRootsProvider
import com.github.saundefined.bitrix_idea.roots.BitrixSourceRootsManager
import com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
import com.github.saundefined.bitrix_idea.toolwindow.BitrixToolWindowManager
import com.github.saundefined.bitrix_idea.ui.LocalizationTable
import com.github.saundefined.bitrix_idea.ui.ModulesTable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiManager
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import javax.swing.JComponent

/**
 * Единая страница настроек Bitrix в `PHP > Frameworks > Bitrix`.
 *
 * Данные вендора и локализации общие для приложения, но редактируются здесь вместе
 * с проектной настройкой framework, чтобы у плагина было одно место настроек.
 */
class BitrixProjectConfigurable(private val project: Project) : Configurable {
    private val settings: BitrixProjectSettings
        get() = BitrixProjectSettings.getInstance(project)

    private val appSettings: AppSettingsState
        get() = AppSettingsState.getInstance()

    private lateinit var panel: DialogPanel
    private lateinit var localizationTable: LocalizationTable
    private lateinit var modulesTable: ModulesTable
    private lateinit var diagnosticsArea: JBTextArea
    private val excludedComponentsField = JBTextField()
    private val excludedTemplatesField = JBTextField()
    private val coreSourceCombo = com.intellij.openapi.ui.ComboBox(BitrixCoreSource.entries.toTypedArray()).apply {
        renderer = SimpleListCellRenderer.create("") { value: BitrixCoreSource -> value.displayName }
    }

    @Suppress("DEPRECATION")
    override fun createComponent(): JComponent {
        localizationTable = LocalizationTable(appSettings.languages)
        modulesTable = ModulesTable(project)
        modulesTable.reset(settings.disabledModules)
        excludedComponentsField.text = settings.excludedComponentIds.joinToString(", ")
        excludedTemplatesField.text = settings.excludedSiteTemplates.joinToString(", ")
        coreSourceCombo.selectedItem = settings.coreSource
        diagnosticsArea = JBTextArea(diagnosticsText()).apply {
            isEditable = false
            isOpaque = false
            lineWrap = true
            rows = 9
            columns = 50
        }

        panel = panel {
            row {
                checkBox(message("settings.bitrix.framework.enabled"))
                    .bindSelected(settings::bitrixFrameworkEnabled)
            }
            row {
                comment(message("settings.bitrix.framework.description"))
            }
            row(message("settings.bitrix.core.source")) {
                cell(coreSourceCombo)
                    .align(AlignX.FILL)
            }.layout(RowLayout.PARENT_GRID)
            row {
                checkBox(message("settings.bitrix.toolwindow"))
                    .bindSelected(settings::bitrixToolWindowEnabled)
            }
            row {
                comment(message("settings.bitrix.toolwindow.description"))
            }
            row {
                checkBox(message("settings.bitrix.lang.folding"))
                    .bindSelected(settings::langFoldingEnabled)
            }
            row(message("settings.bitrix.lang.language")) {
                textField()
                    .bindText(settings::langHintLanguage)
                    .align(AlignX.FILL)
            }.layout(RowLayout.PARENT_GRID)
            row {
                comment(message("settings.bitrix.lang.language.description"))
            }
            row {
                comment(
                    message(
                        "settings.bitrix.version",
                        BitrixProjectStructure.bitrixVersion(project) ?: "—",
                    ),
                )
            }
            row(message("settings.bitrix.root")) {
                textFieldWithBrowseButton(
                    fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
                        .withTitle(message("settings.bitrix.root.choose")),
                    project = project,
                    fileChosen = { file ->
                        val base = project.baseDir
                        if (base != null) {
                            VfsUtilCore.getRelativePath(file, base, '/') ?: file.path
                        } else {
                            file.path
                        }
                    },
                )
                    .bindText(settings::bitrixRootPath)
                    .align(AlignX.FILL)
            }.layout(RowLayout.PARENT_GRID)
            row {
                comment(message("settings.bitrix.root.description"))
            }
            row {
                checkBox(message("settings.bitrix.source.roots"))
                    .bindSelected(settings::autoConfigureSourceRoots)
            }
            row {
                comment(message("settings.bitrix.source.roots.description"))
            }

            collapsibleGroup(message("settings.bitrix.indexing.group"), false) {
                row(message("settings.bitrix.excluded.components")) {
                    cell(excludedComponentsField)
                        .align(AlignX.FILL)
                }.layout(RowLayout.PARENT_GRID)
                row {
                    comment(message("settings.bitrix.excluded.components.description"))
                }
                row(message("settings.bitrix.excluded.templates")) {
                    cell(excludedTemplatesField)
                        .align(AlignX.FILL)
                }.layout(RowLayout.PARENT_GRID)
                row {
                    comment(message("settings.bitrix.excluded.templates.description"))
                }
            }

            collapsibleGroup(message("settings.bitrix.diagnostics.group"), true) {
                row {
                    cell(diagnosticsArea)
                        .align(AlignX.FILL)
                }
                row {
                    button(message("settings.bitrix.reindex")) {
                        reindex()
                    }
                }
            }

            collapsibleGroup(message("settings.modules.group"), false) {
                row {
                    cell(modulesTable.component)
                        .align(AlignX.FILL)
                }
                row {
                    comment(message("settings.modules.description"))
                }
            }

            collapsibleGroup(message("settings.vendor.group"), false) {
                row(message("settings.vendor.code")) {
                    textField()
                        .bindText(appSettings::vendorCode)
                        .align(AlignX.FILL)
                }.layout(RowLayout.PARENT_GRID)

                row(message("settings.vendor.name")) {
                    textField()
                        .bindText(appSettings::vendorName)
                        .align(AlignX.FILL)
                }.layout(RowLayout.PARENT_GRID)

                row(message("settings.vendor.website")) {
                    textField()
                        .bindText(appSettings::vendorWebsite)
                        .align(AlignX.FILL)
                }.layout(RowLayout.PARENT_GRID)
            }

            collapsibleGroup(message("settings.localizations.group"), false) {
                row {
                    label(message("localization.table.row.name"))
                        .align(AlignY.TOP)
                    cell(localizationTable.component)
                        .align(AlignX.FILL)
                }.layout(RowLayout.PARENT_GRID)
            }
        }

        return panel
    }

    override fun isModified(): Boolean {
        if (!::panel.isInitialized) return false
        return panel.isModified() ||
            localizationTable.languages != appSettings.languages ||
            modulesTable.disabledModules != settings.disabledModules ||
            coreSourceCombo.selectedItem != settings.coreSource ||
            parseList(excludedComponentsField.text) != settings.excludedComponentIds ||
            parseList(excludedTemplatesField.text) != settings.excludedSiteTemplates
    }

    override fun apply() {
        if (!::panel.isInitialized) return
        val previousRoot = settings.bitrixRootPath
        val previousDisabled = settings.disabledModules
        val previousCoreSource = settings.coreSource
        panel.apply()
        settings.coreSource = coreSourceCombo.selectedItem as? BitrixCoreSource ?: BitrixCoreSource.BUNDLED
        if (previousCoreSource != settings.coreSource) {
            settings.settingsRevision++
        }
        if (previousRoot != settings.bitrixRootPath) {
            settings.settingsRevision++
        }
        appSettings.languages = localizationTable.languages
        settings.disabledModules = modulesTable.disabledModules.toList()
        val previousComponents = settings.excludedComponentIds
        val previousTemplates = settings.excludedSiteTemplates
        settings.excludedComponentIds = parseList(excludedComponentsField.text)
        settings.excludedSiteTemplates = parseList(excludedTemplatesField.text)
        if (previousDisabled != settings.disabledModules ||
            previousComponents != settings.excludedComponentIds ||
            previousTemplates != settings.excludedSiteTemplates
        ) {
            settings.settingsRevision++
        }
        settings.bitrixDetectionNotificationShown = true
        BitrixLibraryRootsProvider.refresh(project)
        BitrixSourceRootsManager.configure(project)
        BitrixToolWindowManager.refresh(project)
        BitrixToolWindowManager.updateAvailability(project)
    }

    override fun reset() {
        if (!::panel.isInitialized) return
        panel.reset()
        localizationTable.reset(appSettings.languages)
        modulesTable.reset(settings.disabledModules)
        excludedComponentsField.text = settings.excludedComponentIds.joinToString(", ")
        excludedTemplatesField.text = settings.excludedSiteTemplates.joinToString(", ")
        coreSourceCombo.selectedItem = settings.coreSource
        diagnosticsArea.text = diagnosticsText()
    }

    override fun getDisplayName(): String = message("settings.bitrix.framework.title")

    private fun diagnosticsText(): String {
        val snapshot = BitrixDiagnostics.collect(project)
        return buildString {
            appendLine("${message("settings.bitrix.diagnostics.core")}: ${snapshot.coreSource}")
            appendLine("${message("settings.bitrix.diagnostics.core.path")}: ${snapshot.corePath}")
            appendLine("${message("settings.bitrix.diagnostics.version")}: ${snapshot.version}")
            appendLine("${message("settings.bitrix.diagnostics.modules")}: ${snapshot.modulesTotal} (${message("settings.bitrix.diagnostics.indexed")}: ${snapshot.modulesIndexed})")
            appendLine("${message("settings.bitrix.diagnostics.components")}: ${snapshot.components}")
            appendLine("${message("settings.bitrix.diagnostics.templates")}: ${snapshot.templates}")
            appendLine("${message("settings.bitrix.diagnostics.lang")}: ${snapshot.langFiles}")
            if (snapshot.excludedModules.isNotEmpty()) {
                append(message("settings.bitrix.diagnostics.excluded") + ": " + snapshot.excludedModules.joinToString(", "))
            }
        }.trimEnd()
    }

    private fun reindex() {
        settings.settingsRevision++
        ApplicationManager.getApplication().runWriteAction {
            PsiManager.getInstance(project).dropPsiCaches()
        }
        BitrixLibraryRootsProvider.refresh(project)
        BitrixSourceRootsManager.configure(project, force = true)
        BitrixToolWindowManager.refresh(project)
        diagnosticsArea.text = diagnosticsText()
    }

    private fun parseList(value: String): List<String> =
        value.split(',', '\n').map { it.trim() }.filter { it.isNotEmpty() }.distinct()
}
