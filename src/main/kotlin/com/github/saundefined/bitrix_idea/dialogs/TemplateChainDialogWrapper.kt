package com.github.saundefined.bitrix_idea.dialogs

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
import com.github.saundefined.bitrix_idea.templates.component.TemplateChainEntry
import com.github.saundefined.bitrix_idea.templates.component.TemplateChainResolver
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.DefaultListModel
import javax.swing.JComponent

/**
 * Показывает реальную цепочку поиска шаблона компонента.
 */
class TemplateChainDialogWrapper(
    private val project: Project,
    private val componentId: String,
    initialTemplate: String = ".default",
) : DialogWrapper(project, true) {
    private val templateCombo = ComboBox<String>()
    private val model = DefaultListModel<String>()
    private val list = JBList(model)
    private var entries: List<TemplateChainEntry> = emptyList()

    private val contentPanel = panel {
        row(message("template.chain.component")) {
            label(componentId)
        }
        row(message("template.chain.template")) {
            cell(templateCombo)
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row {
            cell(ScrollPaneFactory.createScrollPane(list))
                .align(AlignX.FILL)
        }
        row {
            button(message("template.chain.open")) { openSelected() }
            button(message("template.chain.open.parent")) { openParent() }
        }
    }

    init {
        templateCombo.isEditable = true
        BitrixProjectStructure.availableTemplateNames(project, componentId).sorted().forEach { templateCombo.addItem(it) }
        templateCombo.selectedItem = initialTemplate
        templateCombo.addActionListener { refreshChain() }
        list.cellRenderer = com.intellij.ui.SimpleListCellRenderer.create("") { value: String -> value }
        list.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                if (event.clickCount == 2) openSelected()
            }
        })
        refreshChain()
        init()
        title = message("template.chain.title")
    }

    override fun createCenterPanel(): JComponent = contentPanel.apply {
        preferredSize = Dimension(620, 360)
    }

    private fun refreshChain() {
        val template = (templateCombo.selectedItem as? String)?.trim().orEmpty().ifBlank { ".default" }
        entries = TemplateChainResolver.resolve(project, componentId, template)
        model.clear()
        entries.forEach { entry ->
            val prefix = if (entry.effective) "→ " else "   "
            model.addElement("$prefix${entry.kind}: ${entry.directory.path}")
        }
    }

    private fun openSelected() {
        val index = list.selectedIndex
        val entry = entries.getOrNull(index) ?: entries.firstOrNull() ?: return
        open(entry)
    }

    private fun openParent() {
        val entry = entries.getOrNull(1) ?: return
        open(entry)
    }

    private fun open(entry: TemplateChainEntry) {
        val file = entry.directory.findChild("template.php") ?: entry.directory
        OpenFileDescriptor(project, file).navigate(true)
    }
}
