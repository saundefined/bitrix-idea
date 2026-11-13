package com.github.saundefined.bitrix_idea.toolwindow

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.dialogs.CreateComponentTemplateDialogWrapper
import com.github.saundefined.bitrix_idea.php.BitrixComponentUsages
import com.github.saundefined.bitrix_idea.settings.BitrixProjectConfigurable
import com.github.saundefined.bitrix_idea.settings.BitrixProjectSettings
import com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
import com.intellij.diff.DiffContentFactory
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.find.findUsages.FindUsagesManager
import com.intellij.icons.AllIcons
import com.intellij.refactoring.rename.RenamePsiElementProcessor
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
import com.intellij.openapi.diagnostic.Logger
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
import javax.swing.tree.TreePath
import java.awt.datatransfer.StringSelection
import java.util.concurrent.atomic.AtomicInteger

class BitrixTreePanel(private val project: Project) : Disposable {
    private val log = Logger.getInstance(BitrixTreePanel::class.java)
    private val tree = Tree()
    private val alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)
    private val filterField = JBTextField().apply {
        emptyText.text = message("toolwindow.filter")
    }
    private val generation = AtomicInteger()
    private val properties = PropertiesComponent.getInstance(project)

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
            add(object : ToggleAction(
                message("toolwindow.only.enabled"),
                message("toolwindow.only.enabled.description"),
                AllIcons.Actions.Show,
            ) {
                override fun isSelected(e: AnActionEvent): Boolean =
                    properties.getBoolean(ONLY_ENABLED_KEY, false)

                override fun setSelected(e: AnActionEvent, state: Boolean) {
                    properties.setValue(ONLY_ENABLED_KEY, state, false)
                    refresh()
                }
            })
            add(object : ToggleAction(
                message("toolwindow.group.by.module"),
                message("toolwindow.group.by.module.description"),
                AllIcons.Actions.GroupByModule,
            ) {
                override fun isSelected(e: AnActionEvent): Boolean =
                    properties.getBoolean(GROUP_BY_MODULE_KEY, false)

                override fun setSelected(e: AnActionEvent, state: Boolean) {
                    properties.setValue(GROUP_BY_MODULE_KEY, state, false)
                    refresh()
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
            val model = try {
                ReadAction.compute<TreeNodeData, RuntimeException> { buildTree() }
            } catch (e: Exception) {
                log.warn("Failed to build Bitrix tree", e)
                TreeNodeData(message("toolwindow.load.error"), null)
            }
            ApplicationManager.getApplication().invokeLater {
                if (project.isDisposed || current != generation.get()) return@invokeLater
                tree.setPaintBusy(false)
                val visible = if (query.isEmpty()) model else filterNode(model, query) ?: TreeNodeData("Bitrix", null)
                val root = toSwingNode(visible)
                val expanded = collectExpandedKeys()
                tree.model = DefaultTreeModel(root)
                restoreExpanded(root, TreePath(root), expanded, 0)
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
        data.copyName?.let { copyName ->
            menu.add(JMenuItem(message("toolwindow.copy.name")).apply {
                addActionListener {
                    CopyPasteManager.getInstance().setContents(StringSelection(copyName))
                }
            })
        }
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
        val componentId = data.componentId
        val componentDir = if (componentId != null) {
            com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure.getModel(project)
                .findComponent(componentId)
                ?.directory
        } else {
            file.parent
        }
        if (componentDir != null && (componentDir.findChild("class.php") != null || componentDir.findChild(".description.php") != null)) {
            menu.add(JMenuItem(message("toolwindow.create.template")).apply {
                addActionListener {
                    PsiManager.getInstance(project).findDirectory(componentDir)?.let {
                        CreateComponentTemplateDialogWrapper(project, it).showAndGet()
                    }
                }
            })
        }
        if (componentId != null) {
            menu.add(JMenuItem(message("toolwindow.template.chain")).apply {
                addActionListener {
                    com.github.saundefined.bitrix_idea.dialogs.TemplateChainDialogWrapper(project, componentId).showAndGet()
                }
            })
            menu.add(JMenuItem(message("toolwindow.compare.default")).apply {
                addActionListener { compareWithDefault(componentId) }
            })
            menu.addSeparator()
            menu.add(JMenuItem(message("toolwindow.find.usages")).apply {
                addActionListener { findComponentUsages(componentId) }
            })
            menu.add(JMenuItem(message("toolwindow.rename.component")).apply {
                addActionListener { renameComponent(componentId) }
            })
        }
        menu.show(tree, event.x, event.y)
    }

    private fun findComponentUsages(componentId: String) {
        val literal = BitrixComponentUsages.findFirstComponent(project, componentId) ?: return
        FindUsagesManager(project).findUsages(
            literal,
            literal.containingFile,
            null,
            false,
            null,
        )
    }

    private fun renameComponent(componentId: String) {
        val literal = BitrixComponentUsages.findFirstComponent(project, componentId) ?: return
        RenamePsiElementProcessor.forElement(literal)
            .createRenameDialog(project, literal, literal, null)
            ?.show()
    }

    /**
     * Сравнивает локальный шаблон `.default` с шаблоном из ядра.
     */
    private fun compareWithDefault(componentId: String) {
        val dirs = BitrixProjectStructure.findComponentTemplateDirs(project, componentId, ".default")
        val core = dirs.firstOrNull { it.path.contains("/bitrix/") || it.path.contains("/install/") }
        val local = dirs.firstOrNull { it.path.contains("/local/") }
        if (core == null || local == null) {
            com.intellij.openapi.ui.Messages.showInfoMessage(
                project,
                message("toolwindow.compare.default.not.found"),
                message("toolwindow.compare.default"),
            )
            return
        }
        val coreFile = core.findChild("template.php") ?: core.findChild("style.css")
        val localFile = local.findChild("template.php") ?: local.findChild("style.css")
        if (coreFile == null || localFile == null) {
            com.intellij.openapi.ui.Messages.showInfoMessage(
                project,
                message("toolwindow.compare.default.not.found"),
                message("toolwindow.compare.default"),
            )
            return
        }
        val factory = DiffContentFactory.getInstance()
        val request = SimpleDiffRequest(
            message("toolwindow.compare.default.title", componentId),
            factory.create(project, coreFile),
            factory.create(project, localFile),
            core.path,
            local.path,
        )
        DiffManager.getInstance().showDiff(project, request)
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
        val onlyEnabled = properties.getBoolean(ONLY_ENABLED_KEY, false)
        val modules = (BitrixProjectStructure.allModules(project) +
            com.github.saundefined.bitrix_idea.library.BitrixLibraryRootsProvider.bundledModules())
            .distinctBy { it.id }
            .filter { !onlyEnabled || it.id !in disabled }
            .sortedBy { it.id }
            .map { module ->
                TreeNodeData(
                    label = module.id,
                    file = firstFile(module.directory, "include.php", ".settings.php", "install/version.php"),
                    enabled = module.id !in disabled,
                    copyName = module.id,
                )
            }
        val groupByModule = properties.getBoolean(GROUP_BY_MODULE_KEY, false)
        val components = model.components
            .groupBy { if (groupByModule) it.moduleGroup() else it.vendor }
            .toSortedMap()
            .map { (vendor, items) ->
                TreeNodeData(
                    vendor,
                    null,
                    items.map { component ->
                        TreeNodeData(
                            label = component.id,
                            file = component.entryFiles().firstOrNull()
                                ?: component.directory.findChild(".description.php"),
                            copyName = component.id,
                            componentId = component.id,
                        )
                    },
                )
            }
        val componentTemplateNodes = model.components.take(300).mapNotNull { component ->
            val templateDirs = component.templateDirs()
            if (templateDirs.isEmpty()) return@mapNotNull null
            TreeNodeData(
                label = component.id,
                file = component.entryFiles().firstOrNull(),
                copyName = component.id,
                componentId = component.id,
                children = templateDirs.map { templateDir ->
                    val chain = com.github.saundefined.bitrix_idea.templates.component.TemplateChainResolver
                        .resolve(project, component.id, templateDir.name)
                    TreeNodeData(
                        label = "${templateDir.name}${if (chain.firstOrNull()?.effective == true) " →" else ""}",
                        file = templateDir.findChild("template.php") ?: templateDir,
                        children = chain.map { entry ->
                            TreeNodeData(
                                label = "${if (entry.effective) "→ " else ""}${entry.kind}: ${entry.directory.name}",
                                file = entry.directory.findChild("template.php") ?: entry.directory,
                            )
                        },
                    )
                },
            )
        }

        val templates = model.siteTemplates.map { name ->
            val root = BitrixProjectStructure.bitrixRoot(project)
            val file = root?.findChild("local")?.findChild("templates")?.findChild(name)?.findChild("header.php")
                ?: root?.findChild("bitrix")?.findChild("templates")?.findChild(name)?.findChild("header.php")
            TreeNodeData(name, file, copyName = name)
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
                TreeNodeData(message("toolwindow.template.tree"), null, componentTemplateNodes),
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

    private fun collectExpandedKeys(): Set<String> {
        val root = tree.model?.root as? DefaultMutableTreeNode ?: return emptySet()
        val paths = tree.getExpandedDescendants(TreePath(root)) ?: return emptySet()
        val result = mutableSetOf<String>()
        while (paths.hasMoreElements()) {
            val node = paths.nextElement().lastPathComponent as? DefaultMutableTreeNode ?: continue
            val data = node.userObject as? TreeNodeData ?: continue
            result += nodeKey(data)
        }
        return result
    }

    private fun restoreExpanded(
        node: DefaultMutableTreeNode,
        path: TreePath,
        expanded: Set<String>,
        depth: Int,
    ) {
        val data = node.userObject as? TreeNodeData ?: return
        val shouldExpand = nodeKey(data) in expanded || (expanded.isEmpty() && depth <= 1)
        if (shouldExpand && node.childCount > 0) tree.expandPath(path)
        for (index in 0 until node.childCount) {
            val child = node.getChildAt(index) as? DefaultMutableTreeNode ?: continue
            restoreExpanded(child, path.pathByAddingChild(child), expanded, depth + 1)
        }
    }

    private fun nodeKey(node: TreeNodeData): String = node.label + "|" + (node.file?.path ?: "")

    private companion object {
        const val ONLY_ENABLED_KEY = "bitrix.toolwindow.onlyEnabled"
        const val GROUP_BY_MODULE_KEY = "bitrix.toolwindow.groupByModule"
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
    val copyName: String? = null,
    val componentId: String? = null,
) {
    override fun toString(): String = label
}
