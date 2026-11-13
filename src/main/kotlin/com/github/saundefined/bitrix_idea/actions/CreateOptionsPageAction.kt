package com.github.saundefined.bitrix_idea.actions

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.dialogs.CreateOptionsPageDialogWrapper
import com.github.saundefined.bitrix_idea.orm.OrmEntityContextResolver
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiDirectory

class CreateOptionsPageAction : CreateBitrixFileAction() {
    override fun perform(project: Project, directory: PsiDirectory) {
        if (OrmEntityContextResolver.resolveModuleRoot(directory.virtualFile.path) == null) {
            Messages.showErrorDialog(
                project,
                message("create.options.module.not.found"),
                message("create.options.title"),
            )
            return
        }
        CreateOptionsPageDialogWrapper(project, directory).showAndGet()
    }
}
