package com.github.saundefined.bitrix_idea.ui

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.dialogs.ModuleOptionDialog
import com.github.saundefined.bitrix_idea.templates.module.ModuleOptionSpec
import com.github.saundefined.bitrix_idea.templates.module.ModuleOptionType
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

class ModuleOptionsTable(initial: List<ModuleOptionSpec> = emptyList()) {
    private val options = initial.toMutableList()
    private val tableModel = object : DefaultTableModel(
        arrayOf(
            message("create.options.column.key"),
            message("create.options.column.name"),
            message("create.options.column.type"),
            message("create.options.column.default"),
        ),
        0,
    ) {
        override fun isCellEditable(row: Int, column: Int): Boolean = false
    }
    private val table = JBTable(tableModel).apply {
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        preferredScrollableViewportSize = Dimension(560, 180)
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                if (event.clickCount == 2) editOption()
            }
        })
    }

    val component: JComponent = JBPanel<JBPanel<*>>(BorderLayout()).apply {
        add(ScrollPaneFactory.createScrollPane(table), BorderLayout.CENTER)
        add(createButtonPanel(), BorderLayout.SOUTH)
    }

    val items: List<ModuleOptionSpec>
        get() = options.toList()

    init {
        refresh()
    }

    private fun createButtonPanel(): JBPanel<JBPanel<*>> {
        val addButton = JButton("+").apply { addActionListener { addOption() } }
        val removeButton = JButton("-").apply { addActionListener { removeOption() } }
        val editButton = JButton("...").apply { addActionListener { editOption() } }
        return JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT)).apply {
            add(addButton)
            add(removeButton)
            add(editButton)
        }
    }

    private fun addOption() {
        val dialog = ModuleOptionDialog(
            ModuleOptionSpec("option", "", ModuleOptionType.TEXT),
            options.mapTo(mutableSetOf()) { it.key },
        )
        if (!dialog.showAndGet()) return
        dialog.getOption()?.let {
            options += it
            refresh()
        }
    }

    private fun editOption() {
        val index = table.selectedRow
        if (index !in options.indices) return
        val dialog = ModuleOptionDialog(options[index], options.mapTo(mutableSetOf()) { it.key })
        if (!dialog.showAndGet()) return
        dialog.getOption()?.let {
            options[index] = it
            refresh()
        }
    }

    private fun removeOption() {
        val index = table.selectedRow
        if (index !in options.indices) return
        options.removeAt(index)
        refresh()
    }

    private fun refresh() {
        tableModel.rowCount = 0
        options.forEach {
            tableModel.addRow(arrayOf(it.key, it.name, it.type.displayName, it.defaultValue))
        }
    }
}
