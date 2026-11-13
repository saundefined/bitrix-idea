package com.github.saundefined.bitrix_idea.actions

import com.github.saundefined.bitrix_idea.dialogs.AddAjaxActionDialogWrapper
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.PsiFile

/**
 * Добавляет AJAX-действие в открытый D7-контроллер.
 */
class AddAjaxActionAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val file = event.getData(CommonDataKeys.PSI_FILE) ?: return
        AddAjaxActionDialogWrapper(project, file).showAndGet()
    }

    override fun update(event: AnActionEvent) {
        val file = event.getData(CommonDataKeys.PSI_FILE)
        event.presentation.isEnabledAndVisible = event.project != null && isController(file)
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    private fun isController(file: PsiFile?): Boolean {
        if (file == null || !file.virtualFile?.extension.equals("php", ignoreCase = true)) return false
        val text = file.text
        return text.contains("Engine\\Controller") || text.contains("Engine\\\\Controller")
    }
}
