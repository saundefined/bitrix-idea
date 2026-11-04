package com.github.saundefined.bitrix_idea.dialogs

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.orm.OrmEntityContextResolver
import com.github.saundefined.bitrix_idea.orm.OrmEntityGenerator
import com.github.saundefined.bitrix_idea.orm.OrmEntityModel
import com.github.saundefined.bitrix_idea.orm.OrmEntityValidator
import com.github.saundefined.bitrix_idea.orm.OrmFileWriter
import com.github.saundefined.bitrix_idea.orm.OrmNameUtils
import com.github.saundefined.bitrix_idea.orm.OrmSqlGenerator
import com.github.saundefined.bitrix_idea.templates.BitrixTemplateNames
import com.github.saundefined.bitrix_idea.templates.BitrixTemplateWriter
import com.github.saundefined.bitrix_idea.ui.OrmFieldsTable
import com.github.saundefined.bitrix_idea.ui.OrmRelationsTable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.ui.dsl.builder.*
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JTabbedPane

class CreateOrmEntityDialogWrapper(
    private val project: Project,
    private val directory: PsiDirectory,
) : DialogWrapper(project, true) {
    private val context = OrmEntityContextResolver.resolvePath(directory.virtualFile.path)
    private val fieldsTable = OrmFieldsTable()
    private val relationsTable = OrmRelationsTable { fieldsTable.fields }
    private val tabs = JTabbedPane().apply {
        addTab(message("create.orm.tab.fields"), fieldsTable.component)
        addTab(message("create.orm.tab.relations"), relationsTable.component)
        preferredSize = Dimension(700, 240)
    }

    private var namespace: String = context.namespace
    private var className: String = ""
    private var tableName: String = ""
    private var fullStack: Boolean = true
    private var sqlFiles: Boolean = false
    private var installer: Boolean = true
    private var annotations: Boolean = true
    private var cacheable: Boolean = true
    private var deleteByFilter: Boolean = false
    private var generateEvents: Boolean = false
    private var ufId: String = ""

    init {
        init()
        title = message("create.orm.entity.title")
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row(message("create.orm.entity.namespace")) {
                textField()
                    .bindText(::namespace)
                    .align(AlignX.FILL)
                    .validationOnApply {
                        val error = OrmEntityValidator.namespaceError(it.text.trim())
                        if (error != null) error(message(error)) else null
                    }
            }

            row(message("create.orm.entity.class")) {
                textField()
                    .bindText(::className)
                    .align(AlignX.FILL)
                    .validationOnApply {
                        val error = OrmEntityValidator.classNameError(it.text.trim())
                        if (error != null) error(message(error)) else null
                    }
            }

            row(message("create.orm.entity.table")) {
                textField()
                    .bindText(::tableName)
                    .align(AlignX.FILL)
                    .validationOnApply {
                        val error = OrmEntityValidator.tableNameError(it.text.trim())
                        if (error != null) error(message(error)) else null
                    }
            }

            collapsibleGroup(message("create.orm.options"), true) {
                row {
                    checkBox(message("create.orm.fullstack"))
                        .bindSelected(::fullStack)
                    checkBox(message("create.orm.cacheable"))
                        .bindSelected(::cacheable)
                }
                row {
                    checkBox(message("create.orm.installer"))
                        .bindSelected(::installer)
                    checkBox(message("create.orm.annotations"))
                        .bindSelected(::annotations)
                }
                row {
                    checkBox(message("create.orm.delete.by.filter"))
                        .bindSelected(::deleteByFilter)
                    checkBox(message("create.orm.events"))
                        .bindSelected(::generateEvents)
                }
                row {
                    checkBox(message("create.orm.sql"))
                        .bindSelected(::sqlFiles)
                }
                row(message("create.orm.uf.id")) {
                    textField()
                        .bindText(::ufId)
                        .align(AlignX.FILL)
                }
            }

            row {
                cell(tabs)
                    .align(AlignX.FILL)
            }
        }
    }

    override fun doOKAction() {
        super.applyFields()

        val normalizedClassName = OrmNameUtils.normalizeClassName(className)
        val effectiveTableName = tableName.trim().ifBlank {
            context.tablePrefix + OrmNameUtils.toSnakeCase(normalizedClassName.removeSuffix("Table"))
        }
        val model = OrmEntityModel(
            namespace = namespace.trim(),
            className = normalizedClassName,
            tableName = effectiveTableName,
            fields = fieldsTable.fields,
            relations = relationsTable.relations,
            ufId = ufId.trim().ifBlank { null },
            cacheable = cacheable,
            deleteByFilter = deleteByFilter,
            generateEvents = generateEvents,
        )

        val error = OrmEntityValidator.modelError(model)
        if (error != null) {
            Messages.showErrorDialog(project, message(error), message("create.orm.entity.title"))
            return
        }

        val fileNames = buildList {
            add("${model.className}.php")
            if (fullStack) {
                add("${model.objectClassName}.php")
                add("${model.collectionClassName}.php")
            }
        }
        if (fileNames.any { directory.virtualFile.findChild(it) != null }) {
            Messages.showErrorDialog(
                project,
                message("create.orm.entity.validation.file.exists"),
                message("create.orm.entity.title"),
            )
            return
        }

        val generated = OrmEntityGenerator.generate(model, fullStack)
        BitrixTemplateWriter.write(project) { templateManager, properties ->
            properties.setProperty("NAMESPACE", model.namespace)
            properties.setProperty("CLASS_NAME", model.className)
            properties.setProperty("TABLE_NAME", model.tableName)
            properties.setProperty("IMPORTS", generated.imports)
            properties.setProperty("FIELDS", generated.fields)
            properties.setProperty("FULL_STACK_METHODS", generated.fullStackMethods)
            properties.setProperty("TRAITS", generated.traits)
            properties.setProperty("EXTRA_METHODS", generated.extraMethods)
            properties.setProperty("EVENT_HANDLERS", generated.eventHandlers)

            BitrixTemplateWriter.createFile(
                templateManager,
                directory,
                BitrixTemplateNames.ORM_ENTITY,
                "${model.className}.php",
                properties,
            )

            if (fullStack) {
                writeText("${model.objectClassName}.php", OrmEntityGenerator.generateObjectClass(model))
                writeText("${model.collectionClassName}.php", OrmEntityGenerator.generateCollectionClass(model))
            }

            val moduleRoot = findModuleRoot(directory.virtualFile)
            if (moduleRoot != null) {
                if (installer) {
                    OrmFileWriter.writeEntityList(moduleRoot, model)
                }
                if (annotations) {
                    val relative = VfsUtilCore.getRelativePath(directory.virtualFile, moduleRoot, '/').orEmpty()
                    val path = listOf(relative, "${model.className}.php")
                        .filter { it.isNotBlank() }
                        .joinToString("/")
                    OrmFileWriter.writeAnnotations(moduleRoot, model, path)
                }
            }

            if (sqlFiles) {
                writeSql(model)
            }
        }

        super.doOKAction()
    }

    private fun findModuleRoot(start: VirtualFile): VirtualFile? {
        var current: VirtualFile? = start
        while (current != null) {
            val parent = current.parent ?: return null
            if (parent.name == "modules" && (parent.parent?.name == "local" || parent.parent?.name == "bitrix")) {
                return current
            }
            if (current.name == "local" || current.name == "bitrix") return null
            current = parent
        }
        return null
    }

    private fun writeText(fileName: String, text: String) {
        val file = directory.virtualFile.findChild(fileName)
            ?: directory.virtualFile.createChildData(this, fileName)
        VfsUtil.saveText(file, text)
    }

    private fun writeSql(model: OrmEntityModel) {
        val moduleRoot = findModuleRoot(directory.virtualFile)
            ?: context.moduleRoot?.let { LocalFileSystem.getInstance().findFileByPath(it) }
        if (moduleRoot != null) {
            var directory: VirtualFile = moduleRoot
            listOf("install", "db", "mysql").forEach { part ->
                directory = directory.findChild(part) ?: directory.createChildDirectory(this, part)
            }
            appendSql(directory, "install.sql", OrmSqlGenerator.createTable(model))
            appendSql(directory, "uninstall.sql", OrmSqlGenerator.dropTable(model))
        } else {
            appendSql(directory.virtualFile, "${model.tableName}.sql", OrmSqlGenerator.createTable(model))
            appendSql(directory.virtualFile, "${model.tableName}.drop.sql", OrmSqlGenerator.dropTable(model))
        }
    }

    private fun appendSql(directory: VirtualFile, fileName: String, sql: String) {
        val file = directory.findChild(fileName) ?: directory.createChildData(this, fileName)
        val existing = runCatching { VfsUtilCore.loadText(file) }.getOrDefault("")
        val marker = sql.lineSequence().firstOrNull()?.trim().orEmpty()
        if (marker.isNotEmpty() && existing.contains(marker)) return
        VfsUtil.saveText(file, if (existing.isBlank()) sql else existing.trimEnd() + "\n\n" + sql)
    }
}
