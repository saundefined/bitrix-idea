package com.github.saundefined.bitrix_idea.actions

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.dialogs.CreateComponentTemplateDialogWrapper
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiDirectory

class CreateComponentTemplateAction : CreateBitrixFileAction() {
    override fun perform(project: Project, directory: PsiDirectory) {
        val virtualFile = directory.virtualFile
        if (virtualFile.findChild("class.php") == null && virtualFile.findChild(".description.php") == null) {
            Messages.showErrorDialog(
                project,
                message("create.component.template.not.component"),
                message("create.component.template.title"),
            )
            return
        }
        CreateComponentTemplateDialogWrapper(project, directory).showAndGet()
    }
}
