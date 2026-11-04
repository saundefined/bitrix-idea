package com.github.saundefined.bitrix_idea.actions

import com.github.saundefined.bitrix_idea.dialogs.CreateTemplateDialogWrapper
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory

class CreateTemplateAction : CreateBitrixFileAction() {
    override fun perform(project: Project, directory: PsiDirectory) {
        CreateTemplateDialogWrapper(project, directory).showAndGet()
    }
}
