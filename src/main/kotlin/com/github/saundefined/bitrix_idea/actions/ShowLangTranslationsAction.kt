package com.github.saundefined.bitrix_idea.actions

import com.github.saundefined.bitrix_idea.dialogs.LangTranslationsDialogWrapper
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class ShowLangTranslationsAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val file = event.getData(CommonDataKeys.PSI_FILE) ?: return
        val path = file.virtualFile?.path ?: return
        LangTranslationsDialogWrapper(project, path).showAndGet()
    }

    override fun update(event: AnActionEvent) {
        val file = event.getData(CommonDataKeys.PSI_FILE)
        val path = file?.virtualFile?.path.orEmpty()
        event.presentation.isEnabledAndVisible = event.project != null && path.contains("/lang/")
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
