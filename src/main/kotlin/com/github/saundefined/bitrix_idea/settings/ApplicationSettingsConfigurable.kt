package com.github.saundefined.bitrix_idea.settings

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import java.awt.Dimension
import java.util.*
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader

class ApplicationSettingsConfigurable : Configurable {
    val settings: AppSettingsState
        get() = AppSettingsState().getInstance()

    lateinit var panel: DialogPanel

    override fun createComponent(): JComponent {
        val tableModel = object : DefaultTableModel(
            settings.languages.map { arrayOf(it) }.toTypedArray(),
            arrayOf(message("localization.table.column.name"))
        ) {
            override fun isCellEditable(row: Int, column: Int): Boolean {
                return false
            }
        }

        val table = JBTable(tableModel)
            .apply {
                tableHeader = JTableHeader(this.columnModel).apply {
                    resizingAllowed = false
                    reorderingAllowed = false
                }
            }

        val buttonDimension = Dimension(24, 24)

        val addButton = JButton("+").apply {
            preferredSize = buttonDimension
            minimumSize = buttonDimension
            maximumSize = buttonDimension

            addActionListener {
                val result =
                    JOptionPane.showInputDialog(
                        null,
                        message("localization.add"),
                        message("localization.add.title"),
                        JOptionPane.QUESTION_MESSAGE,
                    )

                if (!result.isNullOrEmpty()) {
                    tableModel.addRow(arrayOf(result))
                    settings.languages = tableModel.dataVector.map { (it as Vector<*>)[0].toString() }
                }
            }
        }

        val removeButton = JButton("-").apply {
            preferredSize = buttonDimension
            minimumSize = buttonDimension
            maximumSize = buttonDimension

            addActionListener {
                if (table.selectedRow != -1 && tableModel.rowCount > 1) {
                    tableModel.removeRow(table.selectedRow)
                    settings.languages = tableModel.dataVector.map { (it as Vector<*>)[0].toString() }
                }
            }
        }

        val btnPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)

            add(addButton)
            add(removeButton)
        }

        val wrapperPanel = JPanel(BorderLayout())
        wrapperPanel.add(btnPanel, BorderLayout.LINE_END)
        wrapperPanel.add(ScrollPaneFactory.createScrollPane(table), BorderLayout.CENTER)

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

            collapsibleGroup(message("settings.localizations.group"), false) {
                row {
                    label(message("localization.table.row.name"))
                        .verticalAlign(VerticalAlign.TOP)
                    cell(wrapperPanel)
                        .horizontalAlign(HorizontalAlign.FILL)
                }
                    .layout(RowLayout.PARENT_GRID)
            }
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