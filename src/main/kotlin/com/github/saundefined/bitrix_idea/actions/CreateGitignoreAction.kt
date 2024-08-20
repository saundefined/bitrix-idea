package com.github.saundefined.bitrix_idea.actions

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.ide.util.DirectoryChooserUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.NotNull

class CreateGitignoreAction : AnAction() {
    override fun actionPerformed(@NotNull event: AnActionEvent) {
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

        val templateManager = FileTemplateManager.getInstance(project)

        ApplicationManager.getApplication().runWriteAction {
            try {
                val template = templateManager.getJ2eeTemplate("Bitrix Module .gitignore")
                FileTemplateUtil.createFromTemplate(template, ".gitignore", null, directory)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}