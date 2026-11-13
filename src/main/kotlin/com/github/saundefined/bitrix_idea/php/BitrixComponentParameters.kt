package com.github.saundefined.bitrix_idea.php

import com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiManager
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression
import com.jetbrains.php.lang.psi.elements.ArrayHashElement
import com.jetbrains.php.lang.psi.elements.AssignmentExpression
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

object BitrixComponentParameters {
    val standardKeys: Set<String> = setOf(
        "CACHE_TYPE", "CACHE_TIME", "CACHE_GROUPS",
        "SET_TITLE", "SET_STATUS_404", "SHOW_404", "MESSAGE_404",
        "SEF_MODE", "SEF_FOLDER", "SEF_URL_TEMPLATES",
        "AJAX_MODE", "AJAX_OPTION_ADDITIONAL", "AJAX_OPTION_HISTORY",
        "AJAX_OPTION_JUMP", "AJAX_OPTION_SHADOW", "AJAX_OPTION_STYLE",
        "COMPONENT_TEMPLATE", "COMPOSITE", "INCLUDE_COMPONENT",
    )

    /**
     * Возвращает null, если компонент или его `.parameters.php` не найден:
     * в этом случае лучше не показывать подсказки и не ругаться инспекцией.
     */
    fun keys(project: Project, componentId: String): Set<String>? {
        val component = BitrixProjectStructure.getModel(project).findComponent(componentId) ?: return null
        val parametersFile = component.directory.findChild(".parameters.php") ?: return null
        val psiFile = PsiManager.getInstance(project).findFile(parametersFile) ?: return null

        return CachedValuesManager.getCachedValue(psiFile, KEY, CachedValueProvider {
            CachedValueProvider.Result.create(parse(psiFile), psiFile)
        })
    }

    private fun parse(psiFile: com.intellij.psi.PsiFile): Set<String>? {
        val assignment = PsiTreeUtil.findChildrenOfType(psiFile, AssignmentExpression::class.java)
            .firstOrNull { it.variable?.text.orEmpty().contains("arComponentParameters") }
            ?: return null
        val rootArray = assignment.value as? ArrayCreationExpression ?: return null
        val parametersElement = rootArray.hashElements.firstOrNull {
            (it.key as? StringLiteralExpression)?.contents == "PARAMETERS"
        } ?: return null
        val parametersArray = parametersElement.value as? ArrayCreationExpression ?: return null

        val result = linkedSetOf<String>()
        parametersArray.hashElements.mapNotNullTo(result) {
            (it.key as? StringLiteralExpression)?.contents
        }
        result += standardKeys
        return result
    }

    private val KEY = Key.create<CachedValue<Set<String>?>>("BitrixComponentParameters")
}
