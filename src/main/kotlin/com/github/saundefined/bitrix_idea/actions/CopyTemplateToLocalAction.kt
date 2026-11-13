package com.github.saundefined.bitrix_idea.actions

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.dialogs.CopyTemplateToLocalDialogWrapper
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiDirectory

class CopyTemplateToLocalAction : CreateBitrixFileAction() {
    override fun perform(project: Project, directory: PsiDirectory) {
        if (directory.virtualFile.parent?.name != "templates") {
            Messages.showErrorDialog(
                project,
                message("refactor.template.not.template"),
                message("refactor.template.title"),
            )
            return
        }
        CopyTemplateToLocalDialogWrapper(project, directory).showAndGet()
    }
}
