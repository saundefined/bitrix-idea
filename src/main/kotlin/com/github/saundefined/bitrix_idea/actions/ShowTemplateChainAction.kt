package com.github.saundefined.bitrix_idea.actions

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.dialogs.TemplateChainDialogWrapper
import com.github.saundefined.bitrix_idea.php.BitrixComponentMeta
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiDirectory

class ShowTemplateChainAction : CreateBitrixFileAction() {
    override fun perform(project: Project, directory: PsiDirectory) {
        val componentId = BitrixComponentMeta.componentIdForFile(directory.virtualFile)
        if (componentId == null) {
            Messages.showErrorDialog(project, message("refactor.component.not.component"), message("template.chain.title"))
            return
        }
        TemplateChainDialogWrapper(project, componentId).showAndGet()
    }
}
