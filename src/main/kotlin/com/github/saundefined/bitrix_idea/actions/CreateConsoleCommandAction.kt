package com.github.saundefined.bitrix_idea.actions

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.dialogs.CreateConsoleCommandDialogWrapper
import com.github.saundefined.bitrix_idea.orm.OrmEntityContextResolver
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiDirectory

class CreateConsoleCommandAction : CreateBitrixFileAction() {
    override fun perform(project: Project, directory: PsiDirectory) {
        if (OrmEntityContextResolver.resolveModuleRoot(directory.virtualFile.path) == null) {
            Messages.showErrorDialog(
                project,
                message("create.admin.module.not.found"),
                message("create.console.title"),
            )
            return
        }
        CreateConsoleCommandDialogWrapper(project, directory).showAndGet()
    }
}
