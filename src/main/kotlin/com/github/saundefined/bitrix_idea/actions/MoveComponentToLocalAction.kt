package com.github.saundefined.bitrix_idea.actions

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.php.BitrixComponentMeta
import com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiDirectory

/**
 * Перемещает компонент из bitrix/components в local/components без смены ID.
 */
class MoveComponentToLocalAction : CreateBitrixFileAction() {
    override fun perform(project: Project, directory: PsiDirectory) {
        val component = directory.virtualFile
        val componentId = BitrixComponentMeta.componentIdForFile(component)
        if (componentId == null) {
            Messages.showErrorDialog(project, message("refactor.component.not.component"), message("refactor.move.local.title"))
            return
        }
        if (component.path.contains("/local/components/")) {
            Messages.showInfoMessage(project, message("refactor.move.local.already"), message("refactor.move.local.title"))
            return
        }
        val root = BitrixProjectStructure.bitrixRoot(project) ?: return
        val target = runCatching {
            WriteCommandAction.runWriteCommandAction(project) {
                val local = root.findChild("local") ?: root.createChildDirectory(this, "local")
                val components = local.findChild("components") ?: local.createChildDirectory(this, "components")
                val vendorName = component.parent?.name.orEmpty()
                if (vendorName.isBlank()) throw IllegalStateException(message("refactor.move.local.error", ""))
                val vendor = components.findChild(vendorName) ?: components.createChildDirectory(this, vendorName)
                if (vendor.findChild(component.name) != null) {
                    throw IllegalStateException(message("refactor.move.local.exists"))
                }
                component.move(this, vendor)
            }
        }
        if (target.isFailure) {
            Messages.showErrorDialog(
                project,
                message("refactor.move.local.error", target.exceptionOrNull()?.message ?: ""),
                message("refactor.move.local.title"),
            )
        }
    }
}
