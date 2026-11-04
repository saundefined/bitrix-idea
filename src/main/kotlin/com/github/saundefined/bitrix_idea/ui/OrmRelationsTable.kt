package com.github.saundefined.bitrix_idea.ui

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.dialogs.OrmRelationDialog
import com.github.saundefined.bitrix_idea.orm.OrmField
import com.github.saundefined.bitrix_idea.orm.OrmRelation
import com.github.saundefined.bitrix_idea.orm.OrmRelationType
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

class OrmRelationsTable(
    private val scalarFieldsProvider: () -> List<OrmField>,
) {
    private val relationList = mutableListOf<OrmRelation>()
    private val tableModel = object : DefaultTableModel(
        arrayOf(
            message("create.orm.relations.column.name"),
            message("create.orm.relations.column.type"),
            message("create.orm.relations.column.target"),
            message("create.orm.relations.column.options"),
        ),
        0,
    ) {
        override fun isCellEditable(row: Int, column: Int): Boolean = false
    }
    private val table = JBTable(tableModel).apply {
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        rowSelectionAllowed = true
        preferredScrollableViewportSize = Dimension(680, 180)
        columnModel.getColumn(0).preferredWidth = 140
        columnModel.getColumn(1).preferredWidth = 100
        columnModel.getColumn(2).preferredWidth = 220
        columnModel.getColumn(3).preferredWidth = 220
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                if (event.clickCount == 2) editRelation()
            }
        })
    }

    val component: JComponent = JBPanel<JBPanel<*>>(BorderLayout()).apply {
        add(ScrollPaneFactory.createScrollPane(table), BorderLayout.CENTER)
        add(createButtonPanel(), BorderLayout.SOUTH)
    }

    val relations: List<OrmRelation>
        get() = relationList.toList()

    init {
        refresh()
    }

    private fun createButtonPanel(): JBPanel<JBPanel<*>> {
        val addButton = JButton("+").apply {
            toolTipText = message("create.orm.relation.add")
            addActionListener { addRelation() }
        }
        val removeButton = JButton("-").apply {
            toolTipText = message("create.orm.relation.remove")
            addActionListener { removeRelation() }
        }
        val editButton = JButton("...").apply {
            toolTipText = message("create.orm.relation.edit")
            addActionListener { editRelation() }
        }

        return JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT)).apply {
            add(addButton)
            add(removeButton)
            add(editButton)
        }
    }

    private fun addRelation() {
        val dialog = OrmRelationDialog(
            initialRelation = OrmRelation(
                name = "",
                type = OrmRelationType.REFERENCE,
                targetEntity = "",
                remoteField = "ID",
            ),
            existingNames = existingNames(),
            scalarFieldNames = scalarFieldNames(),
        )
        if (!dialog.showAndGet()) return

        val relation = dialog.getRelation() ?: return
        relationList += relation
        refresh()
        selectRow(relationList.lastIndex)
    }

    private fun editRelation() {
        val index = table.selectedRow
        if (index !in relationList.indices) return

        val dialog = OrmRelationDialog(
            initialRelation = relationList[index],
            existingNames = existingNames(excludingIndex = index),
            scalarFieldNames = scalarFieldNames(),
        )
        if (!dialog.showAndGet()) return

        val relation = dialog.getRelation() ?: return
        relationList[index] = relation
        refresh()
        selectRow(index)
    }

    private fun removeRelation() {
        val index = table.selectedRow
        if (index !in relationList.indices) return

        relationList.removeAt(index)
        refresh()
        if (relationList.isNotEmpty()) {
            selectRow(index.coerceAtMost(relationList.lastIndex))
        }
    }

    private fun existingNames(excludingIndex: Int? = null): Set<String> {
        return buildSet {
            addAll(scalarFieldsProvider().map { it.name })
            relationList.forEachIndexed { index, relation ->
                if (index != excludingIndex) add(relation.name)
            }
        }
    }

    private fun scalarFieldNames(): Set<String> {
        return scalarFieldsProvider().map { it.name }.toSet()
    }

    private fun refresh() {
        tableModel.rowCount = 0
        relationList.forEach { relation ->
            tableModel.addRow(
                arrayOf(
                    relation.name,
                    relation.type.displayName,
                    relation.targetEntity,
                    relation.optionsSummary(),
                ),
            )
        }
    }

    private fun selectRow(index: Int) {
        if (index in relationList.indices) {
            table.selectionModel.setSelectionInterval(index, index)
        }
    }
}
