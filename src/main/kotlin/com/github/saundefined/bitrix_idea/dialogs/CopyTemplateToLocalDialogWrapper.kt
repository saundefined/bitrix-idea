package com.github.saundefined.bitrix_idea.dialogs

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.php.BitrixComponentMeta
import com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

/**
 * Копирует выбранный шаблон компонента в local/templates/<site>/components/...
 */
class CopyTemplateToLocalDialogWrapper(
    private val project: Project,
    private val templateDirectory: PsiDirectory,
) : DialogWrapper(project, true) {
    private val componentDirectory = BitrixComponentMeta.componentDirectoryFor(templateDirectory.virtualFile)
    private val componentId = componentDirectory?.let { BitrixComponentMeta.componentIdForFile(it) }
    private val templateName = templateDirectory.name
    private val siteCombo = ComboBox<String>()

    private val contentPanel = panel {
        row(message("refactor.template.component")) {
            label(componentId ?: "—")
        }
        row(message("refactor.template.name")) {
            label(templateName)
        }
        row(message("refactor.template.site")) {
            cell(siteCombo)
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row {
            comment(message("refactor.template.hint"))
        }
    }

    init {
        val sites = BitrixProjectStructure.getModel(project).siteTemplates.filter { it != ".default" }
        siteCombo.isEditable = true
        siteCombo.addItem(".default")
        sites.forEach { siteCombo.addItem(it) }
        siteCombo.selectedItem = sites.firstOrNull() ?: ".default"
        init()
        title = message("refactor.template.title")
    }

    override fun createCenterPanel(): JComponent = contentPanel

    override fun doOKAction() {
        val componentDir = componentDirectory
        val component = componentId
        if (componentDir == null || component == null) {
            Messages.showErrorDialog(project, message("refactor.component.not.component"), title)
            return
        }
        val site = (siteCombo.selectedItem as? String)?.trim().orEmpty()
        if (site.isBlank()) return
        val root = BitrixProjectStructure.bitrixRoot(project) ?: return
        val componentPath = component.replaceFirst(':', '/')

        val result = runCatching {
            WriteCommandAction.runWriteCommandAction(project) {
                var parent = ensure(root, "local", "templates", site, "components")
                componentPath.split('/').forEach { part ->
                    parent = parent.findChild(part) ?: parent.createChildDirectory(this, part)
                }
                if (parent.findChild(templateName) != null) {
                    throw IllegalStateException(message("refactor.template.exists"))
                }
                val target = parent.createChildDirectory(this, templateName)
                VfsUtil.copyDirectory(this, templateDirectory.virtualFile, target, null)
            }
        }
        if (result.isFailure) {
            Messages.showErrorDialog(project, result.exceptionOrNull()?.message ?: "", title)
            return
        }
        super.doOKAction()
    }

    private fun ensure(root: VirtualFile, vararg parts: String): VirtualFile {
        var current = root
        parts.forEach { part ->
            current = current.findChild(part) ?: current.createChildDirectory(this, part)
        }
        return current
    }
}
