package com.github.saundefined.bitrix_idea.ui

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.dialogs.OrmValidatorDialog
import com.github.saundefined.bitrix_idea.orm.OrmValidator
import com.github.saundefined.bitrix_idea.orm.OrmValidatorType
import com.intellij.ui.ScrollPaneFactory
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
import com.intellij.ui.components.JBPanel

class OrmValidatorsTable(initial: List<OrmValidator> = emptyList()) {
    private val validators = initial.toMutableList()
    private val tableModel = object : DefaultTableModel(
        arrayOf(
            message("create.orm.validators.column.type"),
            message("create.orm.validators.column.params"),
        ),
        0,
    ) {
        override fun isCellEditable(row: Int, column: Int): Boolean = false
    }
    private val table = JBTable(tableModel).apply {
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        preferredScrollableViewportSize = Dimension(520, 100)
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                if (event.clickCount == 2) editValidator()
            }
        })
    }

    val component: JComponent = JBPanel<JBPanel<*>>(BorderLayout()).apply {
        add(ScrollPaneFactory.createScrollPane(table), BorderLayout.CENTER)
        add(createButtonPanel(), BorderLayout.SOUTH)
    }

    val items: List<OrmValidator>
        get() = validators.toList()

    init {
        refresh()
    }

    private fun createButtonPanel(): JBPanel<JBPanel<*>> {
        val addButton = JButton("+").apply { addActionListener { addValidator() } }
        val removeButton = JButton("-").apply { addActionListener { removeValidator() } }
        val editButton = JButton("...").apply { addActionListener { editValidator() } }
        return JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT)).apply {
            add(addButton)
            add(removeButton)
            add(editButton)
        }
    }

    private fun addValidator() {
        val dialog = OrmValidatorDialog(OrmValidator(OrmValidatorType.LENGTH))
        if (!dialog.showAndGet()) return
        dialog.getValidator()?.let {
            validators += it
            refresh()
        }
    }

    private fun editValidator() {
        val index = table.selectedRow
        if (index !in validators.indices) return
        val dialog = OrmValidatorDialog(validators[index])
        if (!dialog.showAndGet()) return
        dialog.getValidator()?.let {
            validators[index] = it
            refresh()
        }
    }

    private fun removeValidator() {
        val index = table.selectedRow
        if (index !in validators.indices) return
        validators.removeAt(index)
        refresh()
    }

    private fun refresh() {
        tableModel.rowCount = 0
        validators.forEach {
            tableModel.addRow(arrayOf(it.type.displayName, it.paramsSummary()))
        }
    }
}
