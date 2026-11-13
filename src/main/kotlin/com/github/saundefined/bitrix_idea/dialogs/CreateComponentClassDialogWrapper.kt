package com.github.saundefined.bitrix_idea.dialogs

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.templates.component.ComponentClassGenerator
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiDirectory
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class CreateComponentClassDialogWrapper(
    private val project: Project,
    private val directory: PsiDirectory,
) : DialogWrapper(project, true) {
    private var className: String = ComponentClassGenerator.classNameFor(directory.name)
    private var cache: Boolean = true
    private var ajax: Boolean = false
    private var epilog: Boolean = false

    private val contentPanel = panel {
        row(message("create.class.name")) {
            textField()
                .bindText(::className)
                .focused()
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row {
            checkBox(message("create.class.cache"))
                .bindSelected(::cache)
            checkBox(message("create.class.ajax"))
                .bindSelected(::ajax)
            checkBox(message("create.class.epilog"))
                .bindSelected(::epilog)
        }
        row {
            comment(message("create.class.description"))
        }
    }

    init {
        init()
        title = message("create.class.title")
    }

    override fun createCenterPanel(): JComponent = contentPanel

    override fun doValidate(): ValidationInfo? {
        if (!className.matches(Regex("^[A-Z][A-Za-z0-9]*$"))) {
            return ValidationInfo(message("create.class.name.validation"), null)
        }
        return null
    }

    override fun doOKAction() {
        if (directory.virtualFile.findChild("class.php") != null) {
            val result = Messages.showYesNoDialog(
                project,
                message("create.class.exists"),
                title,
                Messages.getQuestionIcon(),
            )
            if (result != Messages.YES) return
        }
        val text = ComponentClassGenerator.generate(className, cache, ajax, epilog)
        if (!GeneratorPreviewDialogWrapper(project, listOf(GeneratorPreviewFile("class.php", text))).showAndGet()) return

        WriteCommandAction.runWriteCommandAction(project) {
            val file = directory.virtualFile.findChild("class.php")
                ?: directory.virtualFile.createChildData(this, "class.php")
            VfsUtil.saveText(file, text)
        }
        super.doOKAction()
    }
}
