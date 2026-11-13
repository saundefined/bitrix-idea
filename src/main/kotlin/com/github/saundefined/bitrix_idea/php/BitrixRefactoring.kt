package com.github.saundefined.bitrix_idea.php

import com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.rename.RenamePsiElementProcessorBase
import com.intellij.usageView.UsageInfo
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

/**
 * Переименование компонентов и шаблонов с обновлением всех `IncludeComponent`.
 */
class BitrixComponentRenameProcessor : RenamePsiElementProcessorBase() {
    override fun canProcessElement(element: PsiElement): Boolean {
        val literal = element as? StringLiteralExpression ?: return false
        return kindOf(literal) != null
    }

    override fun renameElement(
        element: PsiElement,
        newName: String,
        usages: Array<out UsageInfo>,
        listener: RefactoringElementListener?,
    ) {
        val literal = element as? StringLiteralExpression ?: return
        val kind = kindOf(literal) ?: return
        val oldName = literal.contents
        val project = literal.project
        val componentId = componentIdOf(literal)

        val targets = findUsages(project, oldName, kind, componentId)
        ApplicationManager.getApplication().runWriteAction {
            targets.forEach { item ->
                ElementManipulators.handleContentChange(item, item.valueRange, newName)
            }
            when (kind) {
                "component" -> renameComponentDirectory(project, oldName, newName)
                "template" -> renameTemplateDirectories(project, componentId, oldName, newName)
            }
        }
        listener?.elementRenamed(element)
    }

    private fun kindOf(literal: StringLiteralExpression): String? {
        val call = BitrixPhp.directCall(literal) ?: return null
        val index = BitrixPhp.argumentIndex(literal) ?: return null
        return when {
            BitrixPhp.isIncludeComponent(call) && index == 0 -> "component"
            BitrixPhp.isIncludeComponent(call) && index == 1 -> "template"
            else -> null
        }
    }

    private fun componentIdOf(literal: StringLiteralExpression): String? {
        val call = BitrixPhp.directCall(literal) ?: return null
        return (call.parameterList?.getParameter(0) as? StringLiteralExpression)?.contents
    }

    private fun findUsages(
        project: Project,
        value: String,
        kind: String,
        componentId: String?,
    ): List<StringLiteralExpression> {
        val result = mutableListOf<StringLiteralExpression>()
        val scope = GlobalSearchScope.projectScope(project)
        FileTypeIndex.getFiles(PhpFileType.INSTANCE, scope).forEach { virtualFile ->
            val file = PsiManager.getInstance(project).findFile(virtualFile) ?: return@forEach
            PsiTreeUtil.findChildrenOfType(file, StringLiteralExpression::class.java).forEach { literal ->
                if (literal.contents != value) return@forEach
                if (kindOf(literal) != kind) return@forEach
                if (kind == "template" && componentIdOf(literal) != componentId) return@forEach
                result += literal
            }
        }
        return result
    }

    private fun renameComponentDirectory(project: Project, oldName: String, newName: String) {
        if (!newName.contains(":")) return
        val component = BitrixProjectStructure.getModel(project).findComponent(oldName) ?: return
        val directory = component.directory
        val componentsDir = directory.parent?.parent ?: return
        val newVendor = newName.substringBefore(':')
        val newCode = newName.substringAfter(':')
        if (newVendor.isBlank() || newCode.isBlank()) return

        if (directory.parent?.name == newVendor) {
            if (componentsDir.findChild(newVendor)?.findChild(newCode) == null) {
                runCatching { directory.rename(this, newCode) }
            }
            return
        }
        val vendorDirectory = componentsDir.findChild(newVendor)
            ?: runCatching { componentsDir.createChildDirectory(this, newVendor) }.getOrNull()
            ?: return
        if (vendorDirectory.findChild(newCode) != null) return
        runCatching {
            directory.move(this, vendorDirectory)
            directory.rename(this, newCode)
        }
    }

    private fun renameTemplateDirectories(
        project: Project,
        componentId: String?,
        oldName: String,
        newName: String,
    ) {
        if (componentId.isNullOrBlank() || newName.isBlank() || newName.contains('/')) return
        BitrixProjectStructure.findComponentTemplateDirs(project, componentId, oldName).forEach { directory ->
            if (directory.name != oldName) return@forEach
            if (directory.parent?.findChild(newName) != null) return@forEach
            runCatching { directory.rename(this, newName) }
        }
    }
}
