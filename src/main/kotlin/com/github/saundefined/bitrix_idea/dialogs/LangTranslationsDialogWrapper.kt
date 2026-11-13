package com.github.saundefined.bitrix_idea.dialogs

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.settings.AppSettingsState
import com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBPanel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.datatransfer.StringSelection
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.table.DefaultTableModel

/**
 * Таблица переводов lang-ключей текущего файла.
 */
class LangTranslationsDialogWrapper(
    private val project: Project,
    private val sourceFile: VirtualFile,
) : DialogWrapper(project, true) {
    private val languages = AppSettingsState.getInstance().languages.ifEmpty { listOf("ru") }
    private val keys: List<String>
    private val values: Map<String, Map<String, String?>>

    private val tableModel = object : DefaultTableModel(
        emptyArray<Array<Any>>(),
        arrayOf<Any>("KEY", *languages.toTypedArray()),
    ) {
        override fun isCellEditable(row: Int, column: Int): Boolean = column > 0
    }
    private val table = JBTable(tableModel)

    private val contentPanel = panel {
        row {
            cell(ScrollPaneFactory.createScrollPane(table))
                .align(AlignX.FILL)
        }
        row {
            button(message("lang.table.copy.csv")) { copyCsv() }
        }
        row {
            comment(message("lang.table.hint"))
        }
    }

    init {
        val model = BitrixProjectStructure.getModel(project)
        keys = model.langEntries.filterValues { entries -> entries.any { it.file == sourceFile } }
            .keys
            .sorted()
        values = keys.associateWith { key ->
            model.langEntries[key].orEmpty()
                .mapNotNull { entry -> languageOf(entry.file.path)?.let { it to entry.value } }
                .toMap()
        }
        keys.forEach { key ->
            tableModel.addRow(arrayOf<Any>(key, *languages.map { values[key]?.get(it) ?: "" }.toTypedArray()))
        }
        table.preferredScrollableViewportSize = Dimension(720, 360)
        init()
        title = message("lang.table.title")
    }

    override fun createCenterPanel(): JComponent = JBPanel<JBPanel<*>>(BorderLayout()).apply {
        add(contentPanel, BorderLayout.CENTER)
    }

    override fun doOKAction() {
        WriteCommandAction.runWriteCommandAction(project) {
            keys.forEachIndexed { row, key ->
                languages.forEachIndexed { index, language ->
                    val newValue = tableModel.getValueAt(row, index + 1)?.toString().orEmpty()
                    val oldValue = values[key]?.get(language).orEmpty()
                    if (newValue != oldValue) {
                        updateLangFile(key, language, newValue)
                    }
                }
            }
        }
        super.doOKAction()
    }

    private fun updateLangFile(key: String, language: String, newValue: String) {
        val target = targetFile(language) ?: return
        val text = runCatching { VfsUtilCore.loadText(target) }.getOrDefault("<?php\n")
        val escapedKey = Regex.escape(key)
        val pattern = Regex("""(\${'$'}MESS\s*\[\s*['"]$escapedKey['"]\s*=\s*)['"](?:[^'"]*)['"]""")
        val line = "\$MESS['${escape(key)}'] = '${escape(newValue)}';\n"
        val updated = if (pattern.containsMatchIn(text)) {
            text.replace(pattern, "$1'${escape(newValue)}'")
        } else if (text.contains("?>")) {
            text.substringBeforeLast("?>").trimEnd() + "\n" + line
        } else {
            text.trimEnd() + "\n" + line
        }
        VfsUtil.saveText(target, updated)
    }

    private fun targetFile(language: String): VirtualFile? {
        val path = sourceFile.path
        val marker = "/lang/"
        val start = path.indexOf(marker)
        if (start >= 0) {
            val languageEnd = path.indexOf('/', start + marker.length)
            if (languageEnd < 0) return null
            val targetPath = path.substring(0, start + marker.length) + language + path.substring(languageEnd)
            LocalFileSystem.getInstance().findFileByPath(targetPath)?.let { return it }
            val parentPath = targetPath.substringBeforeLast('/')
            val parent = VfsUtil.createDirectoryIfMissing(parentPath) ?: return null
            return parent.findChild(targetPath.substringAfterLast('/')) ?: parent.createChildData(this, targetPath.substringAfterLast('/'))
        }
        val parent = BitrixProjectStructure.findLangTargetParent(sourceFile, language) ?: return null
        return parent.findChild(sourceFile.name) ?: parent.createChildData(this, sourceFile.name)
    }

    private fun escape(value: String): String = value.replace("\\", "\\\\").replace("'", "\\'")

    private fun copyCsv() {
        val csv = buildString {
            appendLine(("KEY" + languages.joinToString(",") { "\"$it\"" }))
            keys.forEach { key ->
                appendLine("\"${key.replace("\"", "\"\"")}\"," + languages.joinToString(",") { language ->
                    "\"${(values[key]?.get(language) ?: "").replace("\"", "\"\"")}\""
                })
            }
        }
        CopyPasteManager.getInstance().setContents(StringSelection(csv))
    }

    private fun languageOf(path: String): String? {
        val marker = "/lang/"
        val start = path.indexOf(marker)
        if (start < 0) return null
        val end = path.indexOf('/', start + marker.length)
        if (end < 0) return null
        return path.substring(start + marker.length, end)
    }
}
