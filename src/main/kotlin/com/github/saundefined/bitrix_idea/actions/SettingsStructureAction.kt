package com.github.saundefined.bitrix_idea.actions

import com.github.saundefined.bitrix_idea.dialogs.SettingsStructureDialogWrapper
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class SettingsStructureAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val file = event.getData(CommonDataKeys.PSI_FILE) ?: return
        if (file.virtualFile?.name != ".settings.php") return
        SettingsStructureDialogWrapper(project, file).showAndGet()
    }

    override fun update(event: AnActionEvent) {
        val file = event.getData(CommonDataKeys.PSI_FILE)
        event.presentation.isEnabledAndVisible = event.project != null && file?.virtualFile?.name == ".settings.php"
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
