package com.github.saundefined.bitrix_idea.dialogs

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.settings.AppSettingsState
import com.github.saundefined.bitrix_idea.templates.BitrixTemplateWriter
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiDirectory
import java.util.Properties

abstract class AbstractCreateDialogWrapper(
    protected val targetProject: Project,
    protected val directory: PsiDirectory,
) : DialogWrapper(targetProject, true) {
    var code: String = ""
    var name: String = ""
    var description: String = ""

    protected val settings: AppSettingsState
        get() = AppSettingsState.getInstance()

    /**
     * Вызывать из init-блока наследника, после инициализации его свойств.
     * Вызов `init()` из конструктора базового класса создаст `createCenterPanel()` слишком рано.
     */
    protected fun initialize(titleKey: String) {
        init()
        title = message(titleKey)
    }

    protected abstract fun createFiles(templateManager: FileTemplateManager, properties: Properties)

    override fun doOKAction() {
        super.applyFields()
        BitrixTemplateWriter.write(targetProject) { templateManager, properties ->
            createFiles(templateManager, properties)
        }
        super.doOKAction()
    }
}
