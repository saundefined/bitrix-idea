package com.github.saundefined.bitrix_idea.php

import com.github.saundefined.bitrix_idea.toolwindow.BitrixHealthIssue
import com.intellij.codeInspection.InspectionManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.jetbrains.php.lang.PhpFileType
import com.github.saundefined.bitrix_idea.php.inspections.DeprecatedApiInspection
import com.github.saundefined.bitrix_idea.php.inspections.DuplicateLangAcrossFilesInspection
import com.github.saundefined.bitrix_idea.php.inspections.LangPlaceholderInspection
import com.github.saundefined.bitrix_idea.php.inspections.MissingComponentTemplateInspection
import com.github.saundefined.bitrix_idea.php.inspections.MissingEventHandlerInspection
import com.github.saundefined.bitrix_idea.php.inspections.MissingIncludeGuardInspection
import com.github.saundefined.bitrix_idea.php.inspections.MissingRequiredComponentParameterInspection
import com.github.saundefined.bitrix_idea.php.inspections.ModuleVersionMismatchInspection
import com.github.saundefined.bitrix_idea.php.inspections.UnknownComponentTemplateInspection
import com.github.saundefined.bitrix_idea.php.inspections.UnknownTemplateParameterInspection

/**
 * Запускает PHP-инспекции плагина для окна Bitrix Health.
 * Вызывается через рефлексию, чтобы окно работало и без PHP-плагина.
 */
object BitrixPhpHealth {
    fun collect(project: Project): List<BitrixHealthIssue> {
        val manager = InspectionManager.getInstance(project)
        val inspections = listOf(
            MissingComponentTemplateInspection(),
            UnknownComponentTemplateInspection(),
            MissingEventHandlerInspection(),
            MissingIncludeGuardInspection(),
            UnknownTemplateParameterInspection(),
            MissingRequiredComponentParameterInspection(),
            LangPlaceholderInspection(),
            ModuleVersionMismatchInspection(),
            DuplicateLangAcrossFilesInspection(),
            DeprecatedApiInspection(),
        )
        val result = mutableListOf<BitrixHealthIssue>()
        val files = FileTypeIndex.getFiles(PhpFileType.INSTANCE, GlobalSearchScope.projectScope(project))
            .take(1000)
        files.forEach { virtualFile ->
            val file = PsiManager.getInstance(project).findFile(virtualFile) ?: return@forEach
            inspections.forEach { inspection ->
                runCatching { manager.defaultProcessFile(inspection, file) }
                    .getOrDefault(emptyList())
                    .forEach { descriptor ->
                        result += BitrixHealthIssue(
                            descriptor.descriptionTemplate,
                            descriptor.psiElement?.containingFile?.virtualFile ?: virtualFile,
                        )
                    }
            }
        }
        return result.distinctBy { it.id }
    }
}
