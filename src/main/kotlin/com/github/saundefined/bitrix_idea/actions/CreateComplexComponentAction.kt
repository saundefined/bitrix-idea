package com.github.saundefined.bitrix_idea.actions

import com.github.saundefined.bitrix_idea.dialogs.CreateComplexComponentDialogWrapper
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory

class CreateComplexComponentAction : CreateBitrixFileAction() {
    override fun perform(project: Project, directory: PsiDirectory) {
        CreateComplexComponentDialogWrapper(project, directory).showAndGet()
    }
}
