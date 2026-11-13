package com.github.saundefined.bitrix_idea.dialogs

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.templates.d7.SettingsFileParser
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiFile
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.treeStructure.Tree
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * Дерево структуры `.settings.php` с переходом к записям.
 */
class SettingsStructureDialogWrapper(
    private val project: Project,
    private val file: PsiFile,
) : DialogWrapper(project, true) {
    private data class Entry(val label: String, val needle: String)

    private val tree = Tree()

    private val contentPanel = panel {
        row {
            cell(ScrollPaneFactory.createScrollPane(tree))
                .align(AlignX.FILL)
        }
        row {
            comment(message("settings.structure.hint"))
        }
    }

    init {
        val model = SettingsFileParser.parse(file.text)
        val root = DefaultMutableTreeNode(message("settings.structure.root"))
        if (model.controllers.isNotEmpty()) {
            val section = DefaultMutableTreeNode(message("settings.editor.controllers"))
            model.controllers.forEach { section.add(DefaultMutableTreeNode(Entry(it, it))) }
            root.add(section)
        }
        if (model.services.isNotEmpty()) {
            val section = DefaultMutableTreeNode(message("settings.editor.services"))
            model.services.forEach { section.add(DefaultMutableTreeNode(Entry(it.key, it.key))) }
            root.add(section)
        }
        if (model.events.isNotEmpty()) {
            val section = DefaultMutableTreeNode(message("settings.editor.events"))
            model.events.forEach { section.add(DefaultMutableTreeNode(Entry("${it.event} → ${it.className}::${it.method}", it.event))) }
            root.add(section)
        }
        if (model.agents.isNotEmpty()) {
            val section = DefaultMutableTreeNode(message("settings.editor.agents"))
            model.agents.forEach { section.add(DefaultMutableTreeNode(Entry(it.callable, it.callable.substringBefore("::")))) }
            root.add(section)
        }
        if (model.console.isNotEmpty()) {
            val section = DefaultMutableTreeNode(message("settings.editor.console"))
            model.console.forEach { section.add(DefaultMutableTreeNode(Entry(it, it))) }
            root.add(section)
        }
        tree.model = DefaultTreeModel(root)
        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                if (event.clickCount == 2) openSelected()
            }
        })
        init()
        title = message("settings.structure.title")
    }

    override fun createCenterPanel(): JComponent = contentPanel

    private fun openSelected() {
        val node = tree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return
        val entry = node.userObject as? Entry ?: return
        val text = file.text
        val offset = text.indexOf(entry.needle, ignoreCase = false)
        val virtualFile = file.virtualFile ?: return
        OpenFileDescriptor(project, virtualFile, offset.coerceAtLeast(0)).navigate(true)
    }
}
