package com.github.saundefined.bitrix_idea.dialogs

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

data class GeneratorPreviewFile(
    val path: String,
    val content: String,
)

/**
 * Показывает содержимое файлов до записи на диск.
 */
class GeneratorPreviewDialogWrapper(
    project: Project,
    private val files: List<GeneratorPreviewFile>,
) : DialogWrapper(project, true) {
    private val fileCombo = ComboBox(files.map { it.path }.toTypedArray())
    private val textArea = JBTextArea(files.firstOrNull()?.content.orEmpty()).apply {
        isEditable = false
        lineWrap = false
        rows = 24
        columns = 90
    }

    private val contentPanel = panel {
        row(message("generator.preview.file")) {
            cell(fileCombo)
                .align(AlignX.FILL)
        }
        row {
            cell(textArea)
                .align(AlignX.FILL)
        }
        row {
            comment(message("generator.preview.description"))
        }
    }

    init {
        fileCombo.addActionListener {
            val index = fileCombo.selectedIndex
            textArea.text = files.getOrNull(index)?.content.orEmpty()
            textArea.caretPosition = 0
        }
        init()
        title = message("generator.preview.title")
    }

    override fun createCenterPanel(): JComponent = contentPanel
}
