package com.github.saundefined.bitrix_idea.php

import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.rename.RenamePsiElementProcessorBase
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class BitrixLangFindUsagesHandlerFactory : FindUsagesHandlerFactory() {
    override fun canFindUsages(element: PsiElement): Boolean {
        val literal = element as? StringLiteralExpression ?: return false
        return BitrixLangSearch.isLangDefinition(literal) ||
            BitrixLangSearch.isLangUsage(literal) ||
            bitrixStringKind(literal) != null
    }

    override fun createFindUsagesHandler(
        element: PsiElement,
        forHighlightUsages: Boolean,
    ): FindUsagesHandler? {
        val literal = element as? StringLiteralExpression ?: return null
        return BitrixLangFindUsagesHandler(literal)
    }
}

private class BitrixLangFindUsagesHandler(
    private val literal: StringLiteralExpression,
) : FindUsagesHandler(literal) {
    override fun processElementUsages(
        element: PsiElement,
        processor: Processor<in UsageInfo>,
        options: FindUsagesOptions,
    ): Boolean {
        val literal = element as? StringLiteralExpression ?: return true
        if (BitrixLangSearch.isLangDefinition(literal) || BitrixLangSearch.isLangUsage(literal)) {
            return BitrixLangSearch.findUsages(element.project, literal.contents).all {
                processor.process(UsageInfo(it))
            }
        }

        val kind = bitrixStringKind(literal) ?: return true
        val key = literal.contents
        val componentId = if (kind == "template") componentIdOf(literal) else null
        val result = BitrixStringUsages.find(element.project, key, kind, componentId)
        return result.all { processor.process(UsageInfo(it)) }
    }
}

private fun bitrixStringKind(literal: StringLiteralExpression): String? {
    val call = BitrixPhp.directCall(literal) ?: return null
    val index = BitrixPhp.argumentIndex(literal) ?: return null
    return when {
        BitrixPhp.isIncludeComponent(call) && index == 0 -> "component"
        BitrixPhp.isIncludeComponent(call) && index == 1 -> "template"
        BitrixPhp.isIncludeModule(call) && index == 0 -> "module"
        BitrixPhp.isEventRegistration(call) && index == 1 -> "event"
        BitrixPhp.isAgentCall(call) && index == 0 -> "agent"
        else -> null
    }
}

private fun componentIdOf(literal: StringLiteralExpression): String? {
    val call = BitrixPhp.directCall(literal) ?: return null
    return (call.parameterList?.getParameter(0) as? StringLiteralExpression)?.contents
}

private object BitrixStringUsages {
    fun find(
        project: com.intellij.openapi.project.Project,
        value: String,
        kind: String,
        componentId: String? = null,
    ): List<StringLiteralExpression> {
        val result = mutableListOf<StringLiteralExpression>()
        val scope = com.intellij.psi.search.GlobalSearchScope.projectScope(project)
        com.intellij.psi.search.FileTypeIndex.getFiles(
            com.jetbrains.php.lang.PhpFileType.INSTANCE,
            scope,
        ).forEach { virtualFile ->
            val text = runCatching { com.intellij.openapi.vfs.VfsUtilCore.loadText(virtualFile) }.getOrNull()
                ?: return@forEach
            if (!text.contains(value)) return@forEach
            val file = com.intellij.psi.PsiManager.getInstance(project).findFile(virtualFile) ?: return@forEach
            com.intellij.psi.util.PsiTreeUtil.findChildrenOfType(file, StringLiteralExpression::class.java)
                .forEach { literal ->
                    if (literal.contents != value) return@forEach
                    if (bitrixStringKind(literal) != kind) return@forEach
                    if (kind == "template" && componentId != null && componentIdOf(literal) != componentId) return@forEach
                    result += literal
                }
        }
        return result
    }
}

class BitrixLangKeyRenameProcessor : RenamePsiElementProcessorBase() {
    override fun canProcessElement(element: PsiElement): Boolean {
        val literal = element as? StringLiteralExpression ?: return false
        return BitrixLangSearch.isLangDefinition(literal) || BitrixLangSearch.isLangUsage(literal)
    }

    override fun renameElement(
        element: PsiElement,
        newName: String,
        usages: Array<out UsageInfo>,
        listener: RefactoringElementListener?,
    ) {
        val literal = element as? StringLiteralExpression ?: return
        val key = literal.contents
        val definitions = BitrixLangSearch.findDefinitions(literal.project, key)
        val references = BitrixLangSearch.findUsages(literal.project, key)

        ApplicationManager.getApplication().runWriteAction {
            (definitions + references).forEach { item ->
                ElementManipulators.handleContentChange(item, item.valueRange, newName)
            }
        }
        listener?.elementRenamed(element)
    }
}
