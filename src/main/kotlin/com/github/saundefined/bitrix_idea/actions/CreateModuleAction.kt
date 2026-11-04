package com.github.saundefined.bitrix_idea.actions

import com.github.saundefined.bitrix_idea.dialogs.CreateModuleDialogWrapper
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory

class CreateModuleAction : CreateBitrixFileAction() {
    override fun perform(project: Project, directory: PsiDirectory) {
        CreateModuleDialogWrapper(project, directory).showAndGet()
    }
}
