package com.github.saundefined.bitrix_idea.settings

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import javax.swing.JComponent

class ApplicationSettingsConfigurable : Configurable {
    val settings: AppSettingsState
        get() = AppSettingsState().getInstance()

    lateinit var panel: DialogPanel

    override fun createComponent(): JComponent {
        panel = panel {
            collapsibleGroup(message("settings.vendor.group"), false) {
                row(message("settings.vendor.code")) {
                    textField()
                        .bindText(settings::vendorCode)
                        .horizontalAlign(HorizontalAlign.FILL)
                }.layout(RowLayout.PARENT_GRID)

                row(message("settings.vendor.name")) {
                    textField()
                        .bindText(settings::vendorName)
                        .horizontalAlign(HorizontalAlign.FILL)
                }.layout(RowLayout.PARENT_GRID)

                row(message("settings.vendor.website")) {
                    textField()
                        .bindText(settings::vendorWebsite)
                        .horizontalAlign(HorizontalAlign.FILL)
                }.layout(RowLayout.PARENT_GRID)
            }
                .expanded = true
        }

        return panel
    }

    override fun isModified(): Boolean {
        return panel.isModified()
    }

    override fun apply() {
        return panel.apply()
    }

    override fun reset() {
        return panel.reset()
    }

    override fun getDisplayName(): String {
        return "Bitrix Idea"
    }
}