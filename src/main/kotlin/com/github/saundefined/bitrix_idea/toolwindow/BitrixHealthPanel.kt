package com.github.saundefined.bitrix_idea.toolwindow

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.settings.BitrixProjectSettings
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.ide.util.PropertiesComponent
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBPanel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.Alarm
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.JComponent
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class BitrixHealthPanel(private val project: Project) : Disposable {
    private val tree = Tree()
    private val alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)
    private val generation = AtomicInteger()
    private val filterField = JBTextField().apply { emptyText.text = message("health.filter") }
    private val properties = PropertiesComponent.getInstance(project)
    private val ignored: MutableSet<String>
        get() = properties.getValue(IGNORED_KEY, "").split('|').filter { it.isNotBlank() }.toMutableSet()

    private val toolbar: ActionToolbar = run {
        val group = DefaultActionGroup().apply {
            add(object : AnAction(message("toolwindow.refresh"), message("toolwindow.refresh"), AllIcons.Actions.Refresh) {
                override fun actionPerformed(e: AnActionEvent) = refresh()
            })
            add(object : AnAction(message("toolwindow.reindex"), message("toolwindow.reindex"), AllIcons.Actions.Restart) {
                override fun actionPerformed(e: AnActionEvent) {
                    BitrixToolWindowManager.reindex(project)
                    BitrixHealthToolWindowManager.refresh(project)
                }
            })
        }
        ActionManager.getInstance().createActionToolbar("BitrixHealthToolWindow", group, true).apply {
            targetComponent = tree
        }
    }

    val component: JComponent = JBPanel<JBPanel<*>>(BorderLayout()).apply {
        val top = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            add(toolbar.component, BorderLayout.NORTH)
            add(filterField, BorderLayout.SOUTH)
        }
        add(top, BorderLayout.NORTH)
        add(ScrollPaneFactory.createScrollPane(tree), BorderLayout.CENTER)
    }

    init {
        tree.showsRootHandles = true
        tree.cellRenderer = object : ColoredTreeCellRenderer() {
            override fun customizeCellRenderer(
                tree: JTree,
                value: Any?,
                selected: Boolean,
                expanded: Boolean,
                leaf: Boolean,
                row: Int,
                hasFocus: Boolean,
            ) {
                val node = (value as? DefaultMutableTreeNode)?.userObject
                if (node is BitrixHealthIssue) {
                    icon = AllIcons.General.Warning
                    append(node.message, SimpleTextAttributes.REGULAR_ATTRIBUTES, true)
                } else {
                    icon = AllIcons.General.InspectionsEye
                    append(node.toString(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES, true)
                }
            }
        }
        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                if (event.clickCount == 2) openSelected()
            }

            override fun mousePressed(event: MouseEvent) = maybeShowPopup(event)
            override fun mouseReleased(event: MouseEvent) = maybeShowPopup(event)
        })
        filterField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent) = refresh()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent) = refresh()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent) = refresh()
        })
        refresh()
    }

    fun refresh() {
        if (project.isDisposed) return
        val settings = BitrixProjectSettings.getInstance(project)
        if (!settings.bitrixFrameworkEnabled || !settings.bitrixHealthWindowEnabled) {
            generation.incrementAndGet()
            tree.model = DefaultTreeModel(DefaultMutableTreeNode(message("health.disabled")))
            return
        }
        val current = generation.incrementAndGet()
        tree.setPaintBusy(true)
        ApplicationManager.getApplication().executeOnPooledThread {
            val issues = ReadAction.compute<Map<String, List<BitrixHealthIssue>>, RuntimeException> {
                val result = BitrixHealthChecker.collect(project).toMutableMap()
                addPhpInspectionIssues(project, result)
                result
            }
            ApplicationManager.getApplication().invokeLater {
                if (project.isDisposed || current != generation.get()) return@invokeLater
                tree.setPaintBusy(false)
                val ignored = ignored
                val query = filterField.text.trim()
                val filtered = issues.mapValues { (_, list) ->
                    list.filter { issue ->
                        issue.id !in ignored && (query.isEmpty() || issue.message.contains(query, ignoreCase = true))
                    }
                }.filterValues { it.isNotEmpty() }
                val total = filtered.values.sumOf { it.size }
                val root = DefaultMutableTreeNode(message("health.root.count", total))
                if (filtered.isEmpty()) {
                    root.add(DefaultMutableTreeNode(message("health.no.issues")))
                } else {
                    filtered.forEach { (category, list) ->
                        val categoryNode = DefaultMutableTreeNode("$category (${list.size})")
                        list.forEach { categoryNode.add(DefaultMutableTreeNode(it)) }
                        root.add(categoryNode)
                    }
                }
                tree.model = DefaultTreeModel(root)
                for (row in 0 until tree.rowCount) tree.expandRow(row)
            }
        }
    }

    private fun addPhpInspectionIssues(project: Project, issues: MutableMap<String, List<BitrixHealthIssue>>) {
        if (!PluginManagerCore.isPluginInstalled(PluginId.getId("com.jetbrains.php"))) return
        runCatching {
            val clazz = Class.forName("com.github.saundefined.bitrix_idea.php.BitrixPhpHealth")
            val method = clazz.getMethod("collect", Project::class.java)
            @Suppress("UNCHECKED_CAST")
            val list = method.invoke(null, project) as? List<BitrixHealthIssue> ?: return@runCatching
            if (list.isNotEmpty()) {
                issues[message("health.category.inspections")] = list
            }
        }
    }

    private fun maybeShowPopup(event: MouseEvent) {
        if (!event.isPopupTrigger) return
        val path = tree.getPathForLocation(event.x, event.y) ?: return
        tree.selectionPath = path
        val node = tree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return
        val issue = node.userObject as? BitrixHealthIssue ?: return
        val menu = JPopupMenu()
        menu.add(JMenuItem(message("health.ignore")).apply {
            addActionListener {
                val current = ignored
                current += issue.id
                properties.setValue(IGNORED_KEY, current.joinToString("|"))
                refresh()
            }
        })
        menu.add(JMenuItem(message("health.clear.ignored")).apply {
            addActionListener {
                properties.unsetValue(IGNORED_KEY)
                refresh()
            }
        })
        menu.show(tree, event.x, event.y)
    }

    private fun openSelected() {
        val node = tree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return
        val issue = node.userObject as? BitrixHealthIssue ?: return
        val file = issue.file ?: return
        OpenFileDescriptor(project, file).navigate(true)
    }

    override fun dispose() {
        alarm.cancelAllRequests()
    }

    private companion object {
        const val IGNORED_KEY = "bitrix.health.ignored"
    }
}
