package com.github.saundefined.bitrix_idea.actions

import com.github.saundefined.bitrix_idea.dialogs.CreateD7ClassDialogWrapper
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory

class CreateD7ClassAction : CreateBitrixFileAction() {
    override fun perform(project: Project, directory: PsiDirectory) {
        CreateD7ClassDialogWrapper(project, directory).showAndGet()
    }
}
