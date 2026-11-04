package com.github.saundefined.bitrix_idea.actions

import com.github.saundefined.bitrix_idea.dialogs.CreateSimpleComponentDialogWrapper
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory

class CreateSimpleComponentAction : CreateBitrixFileAction() {
    override fun perform(project: Project, directory: PsiDirectory) {
        CreateSimpleComponentDialogWrapper(project, directory).showAndGet()
    }
}
