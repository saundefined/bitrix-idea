package com.github.saundefined.bitrix_idea.dialogs

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.orm.OrmEntityContextResolver
import com.github.saundefined.bitrix_idea.structure.BitrixModuleNames
import com.github.saundefined.bitrix_idea.templates.ui.UiGenerators
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiDirectory
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import java.util.Locale
import javax.swing.JComponent

/**
 * Генератор Bitrix UI: grid/filter/form-конфигурации и admin-страницы.
 */
class CreateUiPagesDialogWrapper(
    private val project: Project,
    private val directory: PsiDirectory,
) : DialogWrapper(project, true) {
    private val moduleRootPath = OrmEntityContextResolver.resolveModuleRoot(directory.virtualFile.path)
    private val moduleId = OrmEntityContextResolver.resolveModuleId(directory.virtualFile.path).orEmpty()
    private val namespace = BitrixModuleNames.namespaceFor(moduleId)

    private var entity: String = "Items"
    private var tableClass: String = "ItemsTable"
    private var fields: String = "ID,NAME,ACTIVE"
    private var pageTitle: String = "Список элементов"
    private var modernGrid: Boolean = true

    private val contentPanel = panel {
        row(message("create.ui.module")) {
            label(moduleId.ifBlank { "—" })
        }
        row(message("create.ui.entity")) {
            textField()
                .bindText(::entity)
                .focused()
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row(message("create.ui.table")) {
            textField()
                .bindText(::tableClass)
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row(message("create.ui.fields")) {
            textField()
                .bindText(::fields)
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row(message("create.ui.title")) {
            textField()
                .bindText(::pageTitle)
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row {
            checkBox(message("create.ui.modern.grid"))
                .bindSelected(::modernGrid)
        }
        row {
            comment(message("create.ui.description"))
        }
    }

    init {
        init()
        title = message("create.ui.dialog")
    }

    override fun createCenterPanel(): JComponent = contentPanel

    override fun doValidate(): ValidationInfo? {
        contentPanel.apply()
        if (!entity.matches(Regex("^[A-Z][A-Za-z0-9]*$"))) {
            return ValidationInfo(message("create.ui.entity.validation"), null)
        }
        if (!tableClass.matches(Regex("^[A-Z][A-Za-z0-9]*$"))) {
            return ValidationInfo(message("create.ui.table.validation"), null)
        }
        if (parseFields().isEmpty()) return ValidationInfo(message("create.ui.fields.validation"), null)
        return null
    }

    override fun doOKAction() {
        val root = moduleRootPath?.let { LocalFileSystem.getInstance().findFileByPath(it) }
        if (root == null || moduleId.isBlank()) {
            Messages.showErrorDialog(project, message("create.admin.module.not.found"), title)
            return
        }
        val fieldList = parseFields()
        val files = listOf(
            GeneratorPreviewFile("lib/Ui/${entity}Grid.php", UiGenerators.generateGridConfig(namespace, entity, tableClass, fieldList)),
            GeneratorPreviewFile("lib/Ui/${entity}Filter.php", UiGenerators.generateFilterConfig(namespace, entity, fieldList)),
            GeneratorPreviewFile("lib/Ui/${entity}Form.php", UiGenerators.generateFormConfig(namespace, entity, fieldList)),
            GeneratorPreviewFile(
                "admin/${entity.lowercase(Locale.ROOT)}_list.php",
                if (modernGrid) {
                    UiGenerators.generateModernGridPage(moduleId, namespace, entity, pageTitle)
                } else {
                    UiGenerators.generateListPage(moduleId, namespace, entity, pageTitle)
                },
            ),
            GeneratorPreviewFile("admin/${entity.lowercase(Locale.ROOT)}_edit.php", UiGenerators.generateEditPage(moduleId, namespace, entity, pageTitle)),
        )
        if (!GeneratorPreviewDialogWrapper(project, files).showAndGet()) return

        WriteCommandAction.runWriteCommandAction(project) {
            val libUi = ensureDirectory(root, "lib", "Ui")
            write(libUi, "${entity}Grid.php", files[0].content)
            write(libUi, "${entity}Filter.php", files[1].content)
            write(libUi, "${entity}Form.php", files[2].content)

            val admin = root.findChild("admin") ?: root.createChildDirectory(this, "admin")
            write(admin, "${entity.lowercase(Locale.ROOT)}_list.php", files[3].content)
            write(admin, "${entity.lowercase(Locale.ROOT)}_edit.php", files[4].content)
        }
        super.doOKAction()
    }

    private fun parseFields(): List<String> =
        fields.split(',').map { it.trim().uppercase(Locale.ROOT) }.filter { it.matches(Regex("^[A-Z][A-Z0-9_]*$")) }

    private fun ensureDirectory(root: com.intellij.openapi.vfs.VirtualFile, vararg parts: String): com.intellij.openapi.vfs.VirtualFile {
        var current = root
        parts.forEach { part ->
            current = current.findChild(part) ?: current.createChildDirectory(this, part)
        }
        return current
    }

    private fun write(directory: com.intellij.openapi.vfs.VirtualFile, name: String, content: String) {
        val file = directory.findChild(name) ?: directory.createChildData(this, name)
        VfsUtil.saveText(file, content)
    }
}
