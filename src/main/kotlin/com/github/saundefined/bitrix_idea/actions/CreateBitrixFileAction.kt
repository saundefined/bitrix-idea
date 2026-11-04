package com.github.saundefined.bitrix_idea.actions

import com.intellij.ide.util.DirectoryChooserUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory

abstract class CreateBitrixFileAction : AnAction() {
    final override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val view = event.getData(LangDataKeys.IDE_VIEW) ?: return
        val directory = DirectoryChooserUtil.getOrChooseDirectory(view) ?: return

        perform(project, directory)
    }

    protected abstract fun perform(project: Project, directory: PsiDirectory)

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}
