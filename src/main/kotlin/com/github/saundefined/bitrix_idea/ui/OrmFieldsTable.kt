package com.github.saundefined.bitrix_idea.ui

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.dialogs.OrmFieldDialog
import com.github.saundefined.bitrix_idea.orm.OrmField
import com.github.saundefined.bitrix_idea.orm.OrmFieldType
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.table.DefaultTableModel
import com.intellij.ui.components.JBPanel

class OrmFieldsTable(
    initialFields: List<OrmField> = listOf(defaultIdField()),
) {
    private val fieldList = initialFields.toMutableList()
    private val tableModel = object : DefaultTableModel(
        arrayOf(
            message("create.orm.fields.column.name"),
            message("create.orm.fields.column.type"),
            message("create.orm.fields.column.options"),
            message("create.orm.fields.column.default"),
        ),
        0,
    ) {
        override fun isCellEditable(row: Int, column: Int): Boolean = false
    }
    private val table = JBTable(tableModel).apply {
        setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION)
        rowSelectionAllowed = true
        preferredScrollableViewportSize = Dimension(680, 180)
        columnModel.getColumn(0).preferredWidth = 140
        columnModel.getColumn(1).preferredWidth = 100
        columnModel.getColumn(2).preferredWidth = 300
        columnModel.getColumn(3).preferredWidth = 140
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                if (event.clickCount == 2) editField()
            }
        })
    }

    val component: JComponent = JBPanel<JBPanel<*>>(BorderLayout()).apply {
        add(ScrollPaneFactory.createScrollPane(table), BorderLayout.CENTER)
        add(createButtonPanel(), BorderLayout.SOUTH)
    }

    val fields: List<OrmField>
        get() = fieldList.toList()

    init {
        refresh()
    }

    private fun createButtonPanel(): JBPanel<JBPanel<*>> {
        val addButton = JButton("+").apply {
            toolTipText = message("create.orm.field.add")
            addActionListener { addField() }
        }
        val removeButton = JButton("-").apply {
            toolTipText = message("create.orm.field.remove")
            addActionListener { removeField() }
        }
        val editButton = JButton("...").apply {
            toolTipText = message("create.orm.field.edit")
            addActionListener { editField() }
        }

        return JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT)).apply {
            add(addButton)
            add(removeButton)
            add(editButton)
        }
    }

    private fun addField() {
        val dialog = OrmFieldDialog(
            initialField = OrmField(name = "", type = OrmFieldType.STRING),
            existingNames = fieldList.map { it.name }.toSet(),
        )
        if (!dialog.showAndGet()) return

        val field = dialog.getField() ?: return
        fieldList += field
        refresh()
        selectRow(fieldList.lastIndex)
    }

    private fun editField() {
        val index = table.selectedRow
        if (index !in fieldList.indices) return

        val dialog = OrmFieldDialog(
            initialField = fieldList[index],
            existingNames = fieldList.filterIndexed { fieldIndex, _ -> fieldIndex != index }
                .map { it.name }
                .toSet(),
        )
        if (!dialog.showAndGet()) return

        val field = dialog.getField() ?: return
        fieldList[index] = field
        refresh()
        selectRow(index)
    }

    private fun removeField() {
        val index = table.selectedRow
        if (index !in fieldList.indices) return

        fieldList.removeAt(index)
        refresh()
        if (fieldList.isNotEmpty()) {
            selectRow(index.coerceAtMost(fieldList.lastIndex))
        }
    }

    private fun refresh() {
        tableModel.rowCount = 0
        fieldList.forEach { field ->
            tableModel.addRow(
                arrayOf(
                    field.name,
                    field.type.displayName,
                    field.optionsSummary(),
                    field.defaultValue.orEmpty(),
                ),
            )
        }
    }

    private fun selectRow(index: Int) {
        if (index in fieldList.indices) {
            table.selectionModel.setSelectionInterval(index, index)
        }
    }

    companion object {
        fun defaultIdField(): OrmField {
            return OrmField(
                name = "ID",
                type = OrmFieldType.INTEGER,
                primary = true,
                autocomplete = true,
            )
        }
    }
}
