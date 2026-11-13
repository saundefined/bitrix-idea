package com.github.saundefined.bitrix_idea.dialogs

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.templates.d7.SettingsAgentEntry
import com.github.saundefined.bitrix_idea.templates.d7.SettingsEventEntry
import com.github.saundefined.bitrix_idea.templates.d7.SettingsFileModel
import com.github.saundefined.bitrix_idea.templates.d7.SettingsFileParser
import com.github.saundefined.bitrix_idea.templates.d7.SettingsServiceEntry
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JTabbedPane

/**
 * Структурированный редактор `.settings.php` с предпросмотром результата.
 */
class EditSettingsFileDialogWrapper(
    private val project: Project,
    private val file: PsiFile,
) : DialogWrapper(project, true) {
    private val model = SettingsFileParser.parse(file.text)
    private val controllersArea = area(model.controllers.joinToString("\n"))
    private val servicesArea = area(model.services.joinToString("\n") { "${it.key} = ${it.className}" })
    private val eventsArea = area(
        model.events.joinToString("\n") { "${it.module.ifBlank { "main" }}|${it.event}|${it.className}|${it.method}" },
    )
    private val agentsArea = area(model.agents.joinToString("\n") { "${it.callable}|${it.interval}" })
    private val consoleArea = area(model.console.joinToString("\n"))

    private val tabs = JTabbedPane().apply {
        addTab(message("settings.editor.controllers"), ScrollPaneFactory.createScrollPane(controllersArea))
        addTab(message("settings.editor.services"), ScrollPaneFactory.createScrollPane(servicesArea))
        addTab(message("settings.editor.events"), ScrollPaneFactory.createScrollPane(eventsArea))
        addTab(message("settings.editor.agents"), ScrollPaneFactory.createScrollPane(agentsArea))
        addTab(message("settings.editor.console"), ScrollPaneFactory.createScrollPane(consoleArea))
        preferredSize = Dimension(760, 460)
    }

    private val contentPanel = panel {
        row {
            cell(tabs)
                .align(AlignX.FILL)
        }
        row {
            comment(message("settings.editor.hint"))
        }
    }

    init {
        init()
        title = message("settings.editor.title")
    }

    override fun createCenterPanel(): JComponent = contentPanel

    override fun doOKAction() {
        val updated = SettingsFileModel(
            controllers = lines(controllersArea),
            services = lines(servicesArea).mapNotNull { line ->
                val separator = line.indexOf('=')
                if (separator < 0) return@mapNotNull null
                SettingsServiceEntry(line.substring(0, separator).trim(), line.substring(separator + 1).trim())
            },
            events = lines(eventsArea).mapNotNull { line ->
                val parts = line.split('|')
                if (parts.size < 4) return@mapNotNull null
                SettingsEventEntry(parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim())
            },
            agents = lines(agentsArea).mapNotNull { line ->
                val parts = line.split('|')
                if (parts.size < 2) return@mapNotNull null
                SettingsAgentEntry(parts[0].trim(), parts[1].trim().toIntOrNull() ?: 86400)
            },
            console = lines(consoleArea),
        )
        val rendered = SettingsFileParser.update(file.text, updated)
        if (!GeneratorPreviewDialogWrapper(project, listOf(GeneratorPreviewFile(file.name, rendered))).showAndGet()) return

        WriteCommandAction.runWriteCommandAction(project) {
            val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return@runWriteCommandAction
            document.setText(rendered)
            PsiDocumentManager.getInstance(project).commitDocument(document)
            FileDocumentManager.getInstance().saveDocument(document)
        }
        super.doOKAction()
    }

    private fun area(text: String) = JBTextArea(text).apply {
        lineWrap = false
        rows = 18
        columns = 90
    }

    private fun lines(area: JBTextArea): List<String> =
        area.text.split('\n').map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
}
