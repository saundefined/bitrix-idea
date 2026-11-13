package com.github.saundefined.bitrix_idea.toolwindow

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.dialogs.CreateComponentTemplateDialogWrapper
import com.github.saundefined.bitrix_idea.settings.BitrixProjectConfigurable
import com.github.saundefined.bitrix_idea.settings.BitrixProjectSettings
import com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiDirectory
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBPanel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.Alarm
import com.intellij.util.ui.tree.TreeUtil
import icons.BitrixIdeaIcons
import java.awt.BorderLayout
import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.JTree
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import java.awt.datatransfer.StringSelection
import java.util.concurrent.atomic.AtomicInteger

class BitrixTreePanel(private val project: Project) : Disposable {
    private val tree = Tree()
    private val alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)
    private val filterField = JBTextField().apply {
        emptyText.text = message("toolwindow.filter")
    }
    private val generation = AtomicInteger()

    private val toolbar: ActionToolbar = run {
        val group = DefaultActionGroup().apply {
            add(object : AnAction(message("toolwindow.refresh"), message("toolwindow.refresh"), AllIcons.Actions.Refresh) {
                override fun actionPerformed(e: AnActionEvent) = refresh()
            })
            add(object : AnAction(message("toolwindow.reindex"), message("toolwindow.reindex"), AllIcons.Actions.Restart) {
                override fun actionPerformed(e: AnActionEvent) {
                    BitrixToolWindowManager.reindex(project)
                }
            })
        }
        ActionManager.getInstance().createActionToolbar("BitrixToolWindow", group, true).apply {
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
                val node = (value as? DefaultMutableTreeNode)?.userObject as? TreeNodeData ?: return
                icon = BitrixIdeaIcons.Bitrix
                val attributes = if (node.enabled) {
                    SimpleTextAttributes.REGULAR_ATTRIBUTES
                } else {
                    SimpleTextAttributes.GRAYED_ATTRIBUTES
                }
                append(node.label, attributes, true)
                toolTipText = if (node.enabled) node.label else message("toolwindow.disabled.module", node.label)
            }
        }
        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                if (event.clickCount == 2) navigateToSelection()
            }

            override fun mousePressed(event: MouseEvent) = maybeShowPopup(event)

            override fun mouseReleased(event: MouseEvent) = maybeShowPopup(event)
        })
        filterField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = scheduleRefresh()
            override fun removeUpdate(e: DocumentEvent) = scheduleRefresh()
            override fun changedUpdate(e: DocumentEvent) = scheduleRefresh()
        })
        project.messageBus.connect(this).subscribe(
            VirtualFileManager.VFS_CHANGES,
            object : BulkFileListener {
                override fun after(events: MutableList<out VFileEvent>) {
                    scheduleRefresh()
                }
            },
        )
        refresh()
    }

    fun refresh() {
        if (project.isDisposed) return
        val settings = BitrixProjectSettings.getInstance(project)
        if (!settings.bitrixFrameworkEnabled || !settings.bitrixToolWindowEnabled) {
            generation.incrementAndGet()
            tree.model = DefaultTreeModel(DefaultMutableTreeNode(message("toolwindow.disabled")))
            return
        }
        val current = generation.incrementAndGet()
        val query = filterField.text.trim()
        tree.setPaintBusy(true)
        ApplicationManager.getApplication().executeOnPooledThread {
            val model = ReadAction.compute<TreeNodeData, RuntimeException> { buildTree() }
            ApplicationManager.getApplication().invokeLater {
                if (project.isDisposed || current != generation.get()) return@invokeLater
                tree.setPaintBusy(false)
                val visible = if (query.isEmpty()) model else filterNode(model, query) ?: TreeNodeData("Bitrix", null)
                val root = toSwingNode(visible)
                tree.model = DefaultTreeModel(root)
                TreeUtil.expandAll(tree)
            }
        }
    }

    private fun filterNode(node: TreeNodeData, query: String): TreeNodeData? {
        val children = node.children.mapNotNull { filterNode(it, query) }
        return if (node.label.contains(query, ignoreCase = true) || children.isNotEmpty()) {
            node.copy(children = children)
        } else {
            null
        }
    }

    private fun scheduleRefresh() {
        alarm.cancelAllRequests()
        alarm.addRequest({ refresh() }, 500)
    }

    private fun maybeShowPopup(event: MouseEvent) {
        if (!event.isPopupTrigger) return
        val path = tree.getPathForLocation(event.x, event.y) ?: return
        tree.selectionPath = path
        val node = tree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return
        val data = node.userObject as? TreeNodeData ?: return
        val file = data.file ?: return

        val menu = JPopupMenu()
        menu.add(JMenuItem(message("toolwindow.copy.path")).apply {
            addActionListener {
                CopyPasteManager.getInstance().setContents(StringSelection(file.path))
            }
        })
        menu.add(JMenuItem(message("toolwindow.show.project.view")).apply {
            addActionListener {
                ApplicationManager.getApplication().invokeLater {
                    PsiManager.getInstance(project).findFile(file)?.let {
                        ProjectView.getInstance(project).selectPsiElement(it, true)
                    }
                }
            }
        })
        menu.addSeparator()
        menu.add(JMenuItem(message("toolwindow.settings")).apply {
            addActionListener {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, BitrixProjectConfigurable::class.java)
            }
        })
        val componentDir = file.parent
        if (componentDir != null && (componentDir.findChild("class.php") != null || componentDir.findChild(".description.php") != null)) {
            menu.add(JMenuItem(message("toolwindow.create.template")).apply {
                addActionListener {
                    PsiManager.getInstance(project).findDirectory(componentDir)?.let {
                        CreateComponentTemplateDialogWrapper(project, it).showAndGet()
                    }
                }
            })
        }
        menu.show(tree, event.x, event.y)
    }

    private fun navigateToSelection() {
        val selected = tree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return
        val node = selected.userObject as? TreeNodeData ?: return
        val file = node.file ?: return
        if (!file.isDirectory && file.isValid) {
            OpenFileDescriptor(project, file).navigate(true)
        }
    }

    private fun buildTree(): TreeNodeData {
        val model = BitrixProjectStructure.getModel(project)
        val settings = BitrixProjectSettings.getInstance(project)
        val disabled = settings.disabledModules.toSet()
        val modules = (BitrixProjectStructure.allModules(project) +
            com.github.saundefined.bitrix_idea.library.BitrixLibraryRootsProvider.bundledModules())
            .distinctBy { it.id }
            .sortedBy { it.id }
            .map { module ->
                TreeNodeData(
                    label = module.id,
                    file = firstFile(module.directory, "include.php", ".settings.php", "install/version.php"),
                    enabled = module.id !in disabled,
                )
            }
        val components = model.components
            .groupBy { it.vendor }
            .toSortedMap()
            .map { (vendor, items) ->
                TreeNodeData(
                    vendor,
                    null,
                    items.map { component ->
                        TreeNodeData(component.id, component.entryFiles().firstOrNull())
                    },
                )
            }
        val templates = model.siteTemplates.map { name ->
            val root = BitrixProjectStructure.bitrixRoot(project)
            val file = root?.findChild("local")?.findChild("templates")?.findChild(name)?.findChild("header.php")
                ?: root?.findChild("bitrix")?.findChild("templates")?.findChild(name)?.findChild("header.php")
            TreeNodeData(name, file)
        }

        val codeNodes = BitrixCodeScanner.scan(project)
            .filter { it.value.isNotEmpty() }
            .map { (category, files) ->
                TreeNodeData(
                    category,
                    null,
                    files.map { TreeNodeData(it.label, it.file) },
                )
            }

        val langFiles = model.langFiles.take(500).map { TreeNodeData(it.path, it) }

        return TreeNodeData(
            "Bitrix",
            null,
            listOf(
                TreeNodeData(message("toolwindow.modules"), null, modules),
                TreeNodeData(message("toolwindow.components"), null, components),
                TreeNodeData(message("toolwindow.templates"), null, templates),
                TreeNodeData(message("toolwindow.lang"), null, langFiles),
            ) + codeNodes,
        )
    }

    private fun firstFile(directory: VirtualFile, vararg names: String): VirtualFile? {
        names.forEach { name ->
            directory.findFileByRelativePath(name)?.let { return it }
        }
        return null
    }

    private fun toSwingNode(node: TreeNodeData): DefaultMutableTreeNode {
        val result = DefaultMutableTreeNode(node)
        node.children.forEach { result.add(toSwingNode(it)) }
        return result
    }

    override fun dispose() {
        alarm.cancelAllRequests()
    }
}

private data class TreeNodeData(
    val label: String,
    val file: VirtualFile?,
    val children: List<TreeNodeData> = emptyList(),
    val enabled: Boolean = true,
) {
    override fun toString(): String = label
}
