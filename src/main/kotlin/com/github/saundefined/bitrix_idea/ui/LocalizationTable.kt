package com.github.saundefined.bitrix_idea.ui

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.intellij.openapi.ui.Messages
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBPanel
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import java.awt.Dimension
import java.util.Vector
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader

/**
 * Переиспользуемый редактор списка языков локализации.
 * Используется в диалогах создания и в настройках.
 */
class LocalizationTable(
    initialLanguages: List<String>,
    private val onChanged: () -> Unit = {},
) {
    private val tableModel = object : DefaultTableModel(
        initialLanguages.map { arrayOf(it) }.toTypedArray(),
        arrayOf(message("localization.table.column.name")),
    ) {
        override fun isCellEditable(row: Int, column: Int): Boolean = false
    }

    private val table = JBTable(tableModel).apply {
        tableHeader = JTableHeader(this.columnModel).apply {
            resizingAllowed = false
            reorderingAllowed = false
        }
    }

    val languages: List<String>
        get() = tableModel.dataVector.map { (it as Vector<*>)[0].toString() }

    val component: JComponent = JBPanel<JBPanel<*>>(BorderLayout()).apply {
        add(createButtonPanel(), BorderLayout.LINE_END)
        add(ScrollPaneFactory.createScrollPane(table), BorderLayout.CENTER)
    }

    fun reset(languages: List<String>) {
        tableModel.rowCount = 0
        languages.forEach { tableModel.addRow(arrayOf(it)) }
    }

    private fun createButtonPanel(): JBPanel<JBPanel<*>> {
        val buttonDimension = Dimension(24, 24)

        val addButton = JButton("+").apply {
            preferredSize = buttonDimension
            minimumSize = buttonDimension
            maximumSize = buttonDimension
            addActionListener { addLanguage() }
        }

        val removeButton = JButton("-").apply {
            preferredSize = buttonDimension
            minimumSize = buttonDimension
            maximumSize = buttonDimension
            addActionListener { removeSelectedLanguage() }
        }

        return JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(addButton)
            add(removeButton)
        }
    }

    private fun addLanguage() {
        val result = Messages.showInputDialog(
            message("localization.add"),
            message("localization.add.title"),
            null,
        )

        if (!result.isNullOrEmpty()) {
            tableModel.addRow(arrayOf(result))
            onChanged()
        }
    }

    private fun removeSelectedLanguage() {
        if (table.selectedRow != -1 && tableModel.rowCount > 1) {
            tableModel.removeRow(table.selectedRow)
            onChanged()
        }
    }
}
