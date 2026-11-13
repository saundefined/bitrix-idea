package com.github.saundefined.bitrix_idea.actions

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.dialogs.CreateComponentParametersDialogWrapper
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiDirectory

class CreateComponentParametersAction : CreateBitrixFileAction() {
    override fun perform(project: Project, directory: PsiDirectory) {
        val virtualFile = directory.virtualFile
        if (virtualFile.findChild("class.php") == null &&
            virtualFile.findChild("component.php") == null &&
            virtualFile.findChild(".description.php") == null
        ) {
            Messages.showErrorDialog(
                project,
                message("create.component.template.not.component"),
                message("create.parameters.title"),
            )
            return
        }
        CreateComponentParametersDialogWrapper(project, directory).showAndGet()
    }
}
