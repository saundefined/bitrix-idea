package com.github.saundefined.bitrix_idea.actions

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.dialogs.CreateUiPagesDialogWrapper
import com.github.saundefined.bitrix_idea.orm.OrmEntityContextResolver
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiDirectory

class CreateUiPagesAction : CreateBitrixFileAction() {
    override fun perform(project: Project, directory: PsiDirectory) {
        if (OrmEntityContextResolver.resolveModuleRoot(directory.virtualFile.path) == null) {
            Messages.showErrorDialog(
                project,
                message("create.admin.module.not.found"),
                message("create.ui.dialog"),
            )
            return
        }
        CreateUiPagesDialogWrapper(project, directory).showAndGet()
    }
}
