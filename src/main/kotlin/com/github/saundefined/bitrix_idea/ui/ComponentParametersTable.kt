package com.github.saundefined.bitrix_idea.ui

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.dialogs.ComponentParameterDialog
import com.github.saundefined.bitrix_idea.templates.component.ComponentParameterSpec
import com.github.saundefined.bitrix_idea.templates.component.ComponentParameterType
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBPanel
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.ListSelectionModel
import javax.swing.table.DefaultTableModel

/**
 * Таблица параметров компонента для генератора `.parameters.php`.
 */
class ComponentParametersTable(initial: List<ComponentParameterSpec> = emptyList()) {
    private val parameters = initial.toMutableList()
    private val tableModel = object : DefaultTableModel(
        arrayOf(
            message("create.parameters.column.key"),
            message("create.parameters.column.name"),
            message("create.parameters.column.type"),
            message("create.parameters.column.default"),
        ),
        0,
    ) {
        override fun isCellEditable(row: Int, column: Int): Boolean = false
    }
    private val table = JBTable(tableModel).apply {
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        preferredScrollableViewportSize = Dimension(620, 220)
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                if (event.clickCount == 2) editParameter()
            }
        })
    }

    val component: JComponent = JBPanel<JBPanel<*>>(BorderLayout()).apply {
        add(ScrollPaneFactory.createScrollPane(table), BorderLayout.CENTER)
        add(createButtonPanel(), BorderLayout.SOUTH)
    }

    val items: List<ComponentParameterSpec>
        get() = parameters.toList()

    init {
        refresh()
    }

    private fun createButtonPanel(): JBPanel<JBPanel<*>> {
        val addButton = JButton("+").apply { addActionListener { addParameter() } }
        val removeButton = JButton("-").apply { addActionListener { removeParameter() } }
        val editButton = JButton("...").apply { addActionListener { editParameter() } }
        return JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT)).apply {
            add(addButton)
            add(removeButton)
            add(editButton)
        }
    }

    private fun addParameter() {
        val dialog = ComponentParameterDialog(
            ComponentParameterSpec("NEW_PARAM", "", ComponentParameterType.STRING),
            parameters.mapTo(mutableSetOf()) { it.key },
        )
        if (!dialog.showAndGet()) return
        dialog.getParameter()?.let {
            parameters += it
            refresh()
        }
    }

    private fun editParameter() {
        val index = table.selectedRow
        if (index !in parameters.indices) return
        val dialog = ComponentParameterDialog(parameters[index], parameters.mapTo(mutableSetOf()) { it.key })
        if (!dialog.showAndGet()) return
        dialog.getParameter()?.let {
            parameters[index] = it
            refresh()
        }
    }

    private fun removeParameter() {
        val index = table.selectedRow
        if (index !in parameters.indices) return
        parameters.removeAt(index)
        refresh()
    }

    private fun refresh() {
        tableModel.rowCount = 0
        parameters.forEach {
            tableModel.addRow(arrayOf(it.key, it.name, it.type.displayName, it.defaultValue))
        }
    }
}
