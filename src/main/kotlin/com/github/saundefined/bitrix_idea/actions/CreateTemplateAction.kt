package com.github.saundefined.bitrix_idea.actions

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import icons.license.CheckLicense
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.jetbrains.annotations.NotNull


class CreateTemplateAction : AnAction() {
    override fun actionPerformed(@NotNull event: AnActionEvent) {
        if (CheckLicense.isLicensed == false) {
            CheckLicense.requestLicense(message("license.request"))
            return
        }
    }
    
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}