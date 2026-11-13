package com.github.saundefined.bitrix_idea.php

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
import com.intellij.navigation.GotoRelatedItem
import com.intellij.navigation.GotoRelatedProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

/**
 * `Navigate > Related Symbol` для компонентов и lang-фраз.
 */
class BitrixGotoRelatedProvider : GotoRelatedProvider() {
    override fun getItems(element: PsiElement): List<GotoRelatedItem> {
        val project = element.project
        val virtualFile = element.containingFile?.virtualFile

        val literal = element as? StringLiteralExpression
            ?: PsiTreeUtil.getParentOfType(element, StringLiteralExpression::class.java, false)
        if (literal != null && BitrixLangSearch.isLangDefinition(literal)) {
            return BitrixProjectStructure.findLangEntries(project, literal.contents)
                .mapNotNull { BitrixProjectStructure.psiElementAt(project, it) }
                .filter { it != element }
                .distinctBy { it.containingFile?.virtualFile?.path + ":" + it.textOffset }
                .map { GotoRelatedItem(it, message("goto.related.lang")) }
        }

        if (virtualFile == null) return emptyList()
        val componentId = BitrixComponentMeta.componentIdForFile(virtualFile) ?: return emptyList()
        val component = BitrixProjectStructure.getModel(project).findComponent(componentId) ?: return emptyList()

        val files = buildList {
            addAll(component.entryFiles())
            listOf(".description.php", ".parameters.php").forEach { name ->
                component.directory.findChild(name)?.let { add(it) }
            }
            component.templateDirs().forEach { template ->
                template.findChild("template.php")?.let { add(it) }
            }
        }

        return files
            .filter { it != virtualFile }
            .distinct()
            .mapNotNull { BitrixTargets.psiFile(project, it) }
            .map { GotoRelatedItem(it, message("goto.related.component")) }
    }
}
