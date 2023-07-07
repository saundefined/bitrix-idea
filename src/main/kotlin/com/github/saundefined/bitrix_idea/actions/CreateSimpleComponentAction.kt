package com.github.saundefined.bitrix_idea.actions

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.dialogs.CreateSimpleComponentDialogWrapper
import com.intellij.ide.util.DirectoryChooserUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.project.Project
import icons.license.CheckLicense
import org.jetbrains.annotations.NotNull


class CreateSimpleComponentAction : AnAction() {
    override fun actionPerformed(@NotNull event: AnActionEvent) {
        if (CheckLicense.isLicensed == false) {
            CheckLicense.requestLicense(message("license.request"))
            return
        }

        val view = event.getData(LangDataKeys.IDE_VIEW)
        if (view === null) {
            return
        }

        val project: Project? = event.project
        if (project === null) {
            return
        }

        val directory = DirectoryChooserUtil.getOrChooseDirectory(view)
        if (directory === null) {
            return
        }

        val dialogWrapper = CreateSimpleComponentDialogWrapper()
        dialogWrapper.view = view
        dialogWrapper.project = project
        dialogWrapper.directory = directory
        dialogWrapper.showAndGet()
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}