package com.github.saundefined.bitrix_idea.actions

import com.github.saundefined.bitrix_idea.templates.BitrixTemplateNames
import com.github.saundefined.bitrix_idea.templates.BitrixTemplateWriter
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory

class CreateGitignoreAction : CreateBitrixFileAction() {
    override fun perform(project: Project, directory: PsiDirectory) {
        BitrixTemplateWriter.write(project) { templateManager, _ ->
            BitrixTemplateWriter.createFile(
                templateManager,
                directory,
                BitrixTemplateNames.MODULE_GITIGNORE,
                ".gitignore",
            )
        }
    }
}
