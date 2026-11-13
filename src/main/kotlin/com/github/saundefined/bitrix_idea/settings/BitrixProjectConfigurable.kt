package com.github.saundefined.bitrix_idea.settings

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.library.BitrixLibraryRootsProvider
import com.github.saundefined.bitrix_idea.roots.BitrixSourceRootsManager
import com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
import com.github.saundefined.bitrix_idea.toolwindow.BitrixToolWindowManager
import com.github.saundefined.bitrix_idea.ui.LocalizationTable
import com.github.saundefined.bitrix_idea.ui.ModulesTable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.vfs.VfsUtilCore
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

    @Suppress("DEPRECATION")
    override fun createComponent(): JComponent {
        localizationTable = LocalizationTable(appSettings.languages)
        modulesTable = ModulesTable(project)
        modulesTable.reset(settings.disabledModules)

        panel = panel {
            row {
                checkBox(message("settings.bitrix.framework.enabled"))
                    .bindSelected(settings::bitrixFrameworkEnabled)
            }
            row {
                comment(message("settings.bitrix.framework.description"))
            }
            row {
                checkBox(message("settings.bitrix.use.bundled.library"))
                    .bindSelected(settings::useBundledLibrary)
            }
            row {
                checkBox(message("settings.bitrix.toolwindow"))
                    .bindSelected(settings::bitrixToolWindowEnabled)
            }
            row {
                comment(message("settings.bitrix.toolwindow.description"))
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
            modulesTable.disabledModules != settings.disabledModules
    }

    override fun apply() {
        if (!::panel.isInitialized) return
        val previousRoot = settings.bitrixRootPath
        val previousDisabled = settings.disabledModules
        panel.apply()
        if (previousRoot != settings.bitrixRootPath) {
            settings.settingsRevision++
        }
        appSettings.languages = localizationTable.languages
        settings.disabledModules = modulesTable.disabledModules.toList()
        if (previousDisabled != settings.disabledModules) {
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
    }

    override fun getDisplayName(): String = message("settings.bitrix.framework.title")
}
