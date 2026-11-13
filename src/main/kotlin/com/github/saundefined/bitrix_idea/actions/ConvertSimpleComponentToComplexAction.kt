package com.github.saundefined.bitrix_idea.actions

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.php.BitrixComponentMeta
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiDirectory

/**
 * Добавляет component.php, превращая простой компонент в комплексный.
 */
class ConvertSimpleComponentToComplexAction : CreateBitrixFileAction() {
    override fun perform(project: Project, directory: PsiDirectory) {
        val component = directory.virtualFile
        if (BitrixComponentMeta.componentIdForFile(component) == null) {
            Messages.showErrorDialog(project, message("refactor.component.not.component"), message("refactor.complex.title"))
            return
        }
        if (component.findChild("component.php") != null) {
            Messages.showInfoMessage(project, message("refactor.complex.exists"), message("refactor.complex.title"))
            return
        }
        WriteCommandAction.runWriteCommandAction(project) {
            VfsUtil.saveText(
                component.createChildData(this, "component.php"),
                "<?php\n\nif (!defined('B_PROLOG_INCLUDED') || B_PROLOG_INCLUDED !== true) {\n    die();\n}\n\nrequire_once __DIR__.'/class.php';\n",
            )
        }
    }
}
