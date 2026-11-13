package com.github.saundefined.bitrix_idea.dialogs

import com.intellij.openapi.command.WriteCommandAction

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.php.BitrixComponentMeta
import com.github.saundefined.bitrix_idea.settings.AppSettingsState
import com.github.saundefined.bitrix_idea.templates.component.ComponentParameterSpec
import com.github.saundefined.bitrix_idea.templates.component.ComponentParameterType
import com.github.saundefined.bitrix_idea.templates.component.ComponentParametersWriter
import com.github.saundefined.bitrix_idea.ui.ComponentParametersTable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiDirectory
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

/**
 * Генератор `.parameters.php` и lang-файлов по таблице параметров.
 */
class CreateComponentParametersDialogWrapper(
    private val project: Project,
    private val directory: PsiDirectory,
) : DialogWrapper(project, true) {
    private val componentId = BitrixComponentMeta.componentIdForFile(directory.virtualFile)
    private val table = ComponentParametersTable(initialParameters())

    private val contentPanel = panel {
        row {
            comment(message("create.parameters.description"))
        }
        row {
            cell(table.component)
                .align(AlignX.FILL)
        }
    }

    init {
        init()
        title = message("create.parameters.title")
    }

    override fun createCenterPanel(): JComponent = contentPanel

    override fun doOKAction() {
        val items = table.items
        if (items.isEmpty()) {
            Messages.showErrorDialog(project, message("create.parameters.empty"), title)
            return
        }
        val languages = AppSettingsState.getInstance().languages.ifEmpty { listOf("ru") }
        val preview = listOf(
            GeneratorPreviewFile(".parameters.php", ComponentParametersWriter.generate(items)),
        ) + languages.map { language ->
            GeneratorPreviewFile("lang/$language/.parameters.php", ComponentParametersWriter.generateLang(items))
        }
        if (!GeneratorPreviewDialogWrapper(project, preview).showAndGet()) return

        WriteCommandAction.runWriteCommandAction(project) {
            ComponentParametersWriter.write(
                componentDirectory = directory.virtualFile,
                parameters = items,
                languages = languages,
            )
        }
        super.doOKAction()
    }

    private fun initialParameters(): List<ComponentParameterSpec> {
        val id = componentId ?: return emptyList()
        return BitrixComponentMeta.parameters(project, id).values.map { parameter ->
            ComponentParameterSpec(
                key = parameter.key,
                name = parameter.name ?: parameter.key,
                type = ComponentParameterType.entries.firstOrNull { it.displayName == parameter.type }
                    ?: ComponentParameterType.STRING,
                defaultValue = parameter.defaultValue.orEmpty(),
                parent = parameter.parent?.ifBlank { "BASE" } ?: "BASE",
            )
        }
    }
}