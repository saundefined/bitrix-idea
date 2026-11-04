package com.github.saundefined.bitrix_idea.ui

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.library.BitrixLibraryRootsProvider
import com.github.saundefined.bitrix_idea.structure.BitrixModuleInfo
import com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
import com.intellij.openapi.project.Project
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader
import com.intellij.ui.components.JBPanel

/**
 * Фильтр индексации модулей для настроек проекта. По умолчанию включены все модули,
 * включая встроенную библиотеку `bitrix-idea-library`.
 */
class ModulesTable(private val project: Project) {
    private val tableModel = object : DefaultTableModel(
        emptyArray<Array<Any>>(),
        arrayOf(
            message("settings.modules.column.module"),
            message("settings.modules.column.source"),
            message("settings.modules.column.index"),
        ),
    ) {
        override fun getColumnClass(columnIndex: Int): Class<*> {
            return if (columnIndex == 2) java.lang.Boolean::class.java else String::class.java
        }

        override fun isCellEditable(row: Int, column: Int): Boolean = column == 2
    }

    private val table = JBTable(tableModel).apply {
        tableHeader = JTableHeader(this.columnModel).apply {
            resizingAllowed = false
            reorderingAllowed = false
        }
        preferredScrollableViewportSize = Dimension(480, 180)
    }

    val component: JComponent = JBPanel<JBPanel<*>>(BorderLayout()).apply {
        add(ScrollPaneFactory.createScrollPane(table), BorderLayout.CENTER)
        add(createButtonPanel(), BorderLayout.SOUTH)
    }

    val disabledModules: List<String>
        get() = (0 until tableModel.rowCount).mapNotNull<Int, String> { row ->
            val value = tableModel.getValueAt(row, 2)
            val enabled = value == true || value.toString().equals("true", ignoreCase = true)
            if (enabled) null else tableModel.getValueAt(row, 0).toString()
        }

    private fun createButtonPanel(): JBPanel<JBPanel<*>> {
        return JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT)).apply {
            add(JButton(message("settings.modules.select.all")).apply {
                addActionListener { setAll(true) }
            })
            add(JButton(message("settings.modules.select.none")).apply {
                addActionListener { setAll(false) }
            })
        }
    }

    private fun setAll(enabled: Boolean) {
        for (row in 0 until tableModel.rowCount) {
            tableModel.setValueAt(enabled, row, 2)
        }
    }

    fun reset(disabled: List<String>) {
        tableModel.rowCount = 0
        val modules = (BitrixProjectStructure.allModules(project) + BitrixLibraryRootsProvider.bundledModules())
            .distinctBy { it.id }
            .sortedBy { it.id }
        modules.forEach { module ->
            tableModel.addRow(arrayOf<Any>(module.id, sourceLabel(module), module.id !in disabled))
        }
    }

    private fun sourceLabel(module: BitrixModuleInfo): String {
        return when {
            module.bundled -> message("settings.modules.source.library")
            module.local -> message("settings.modules.source.local")
            else -> message("settings.modules.source.bitrix")
        }
    }
}
