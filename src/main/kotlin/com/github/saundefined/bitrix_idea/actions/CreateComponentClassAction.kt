package com.github.saundefined.bitrix_idea.actions

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.dialogs.CreateComponentClassDialogWrapper
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiDirectory

class CreateComponentClassAction : CreateBitrixFileAction() {
    override fun perform(project: Project, directory: PsiDirectory) {
        val virtualFile = directory.virtualFile
        val isComponent = virtualFile.parent?.parent?.name == "components" ||
            virtualFile.findChild(".description.php") != null ||
            virtualFile.findChild(".parameters.php") != null
        if (!isComponent) {
            Messages.showErrorDialog(
                project,
                message("create.component.template.not.component"),
                message("create.class.title"),
            )
            return
        }
        CreateComponentClassDialogWrapper(project, directory).showAndGet()
    }
}
