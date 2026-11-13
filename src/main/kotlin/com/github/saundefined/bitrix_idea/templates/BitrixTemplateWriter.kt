package com.github.saundefined.bitrix_idea.templates

import com.intellij.openapi.command.WriteCommandAction

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import java.util.Properties

object BitrixTemplateWriter {
    private val LOG = Logger.getInstance(BitrixTemplateWriter::class.java)

    /**
     * Выполняет [action] внутри write action с приватной копией свойств шаблонов IDE.
     * Копия не даёт диалогам менять общий `FileTemplateManager.defaultProperties`.
     */
    fun write(project: Project, action: (FileTemplateManager, Properties) -> Unit) {
        val templateManager = FileTemplateManager.getInstance(project)
        val properties = Properties().apply { putAll(templateManager.defaultProperties) }

        WriteCommandAction.runWriteCommandAction(project) {
            try {
                action(templateManager, properties)
            } catch (e: Exception) {
                LOG.error("Failed to create files from Bitrix file templates", e)
            }
        }
    }

    fun createFile(
        templateManager: FileTemplateManager,
        directory: PsiDirectory,
        templateName: String,
        fileName: String,
        properties: Properties? = null,
    ) {
        val template = templateManager.getJ2eeTemplate(templateName)
        FileTemplateUtil.createFromTemplate(template, fileName, properties, directory)
    }

    fun createFiles(
        templateManager: FileTemplateManager,
        directory: PsiDirectory,
        templateNamePrefix: String,
        fileNames: List<String>,
        properties: Properties? = null,
    ) {
        fileNames.forEach { fileName ->
            createFile(templateManager, directory, "$templateNamePrefix $fileName", fileName, properties)
        }
    }
}