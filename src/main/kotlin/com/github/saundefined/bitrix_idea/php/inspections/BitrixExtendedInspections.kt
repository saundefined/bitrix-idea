package com.github.saundefined.bitrix_idea.php.inspections

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.php.BitrixClassResolver
import com.github.saundefined.bitrix_idea.php.BitrixComponentMeta
import com.github.saundefined.bitrix_idea.php.BitrixComponentParameters
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.ArrayAccessExpression
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.php.lang.psi.elements.Variable
import com.jetbrains.php.lang.psi.visitors.PhpElementVisitor

/**
 * Параметр в шаблоне, которого нет в `.parameters.php` компонента.
 */
class UnknownTemplateParameterInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PhpElementVisitor() {
            override fun visitPhpStringLiteralExpression(element: StringLiteralExpression) {
                val arrayAccess = PsiTreeUtil.getParentOfType(element, ArrayAccessExpression::class.java, false) ?: return
                if (arrayAccess.index?.value !== element) return
                val variable = arrayAccess.value as? Variable ?: return
                if (variable.name != "arParams") return

                val file = element.containingFile.virtualFile ?: return
                val componentId = BitrixComponentMeta.componentIdForFile(file) ?: return
                val known = BitrixComponentParameters.keys(element.project, componentId) ?: return
                if (element.contents in known) return

                val componentDirectory = BitrixComponentMeta.componentDirectoryFor(file)
                holder.registerProblem(
                    element,
                    message("inspection.template.parameter.message", element.contents),
                    ProblemHighlightType.WEAK_WARNING,
                    *listOfNotNull(
                        componentDirectory?.takeIf { it.findChild(".parameters.php") != null }?.let {
                            AddComponentParameterFix(it, element.contents, element.contents)
                        },
                    ).toTypedArray(),
                )
            }
        }
    }
}

/**
 * В `IncludeComponent` указан шаблон, которого нет у компонента.
 */
class UnknownComponentTemplateInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PhpElementVisitor() {
            override fun visitPhpStringLiteralExpression(element: StringLiteralExpression) {
                val call = com.github.saundefined.bitrix_idea.php.BitrixPhp.directCall(element) ?: return
                val index = com.github.saundefined.bitrix_idea.php.BitrixPhp.argumentIndex(element) ?: return
                if (!com.github.saundefined.bitrix_idea.php.BitrixPhp.isIncludeComponent(call) || index != 1) return

                val componentLiteral = call.parameterList?.getParameter(0) as? StringLiteralExpression ?: return
                val componentId = componentLiteral.contents
                val template = element.contents.ifBlank { ".default" }
                if (componentId.isBlank()) return
                val component = com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
                    .getModel(element.project)
                    .findComponent(componentId) ?: return
                val dirs = com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
                    .findComponentTemplateDirs(element.project, componentId, template)
                if (dirs.isNotEmpty()) return

                holder.registerProblem(
                    element,
                    message("inspection.component.template.unknown.message", componentId, template),
                    ProblemHighlightType.WEAK_WARNING,
                    CreateComponentTemplateDirectoryFix(component.directory, template),
                )
            }
        }
    }
}

/**
 * Параметр объявлен в `.parameters.php`, но нигде не используется в компоненте.
 */
class UnusedComponentParameterInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PhpElementVisitor() {
            override fun visitPhpFile(file: PhpFile) {
                val virtualFile = file.virtualFile ?: return
                if (virtualFile.name != ".parameters.php") return
                val componentId = BitrixComponentMeta.componentIdForFile(virtualFile) ?: return
                val componentDirectory = BitrixComponentMeta.componentDirectoryFor(virtualFile) ?: return
                val declared = BitrixComponentMeta.parameters(file.project, componentId)
                if (declared.isEmpty()) return

                val used = collectUsedParameters(componentDirectory)
                declared.values.forEach { parameter ->
                    if (parameter.key in BitrixComponentParameters.standardKeys) return@forEach
                    if (parameter.key in used) return@forEach
                    holder.registerProblem(
                        parameter.target,
                        message("inspection.unused.parameter.message", parameter.key),
                        ProblemHighlightType.WEAK_WARNING,
                    )
                }
            }
        }
    }

    private fun collectUsedParameters(directory: VirtualFile): Set<String> {
        val result = linkedSetOf<String>()
        val pattern = Regex("""\${'$'}arParams\s*\[\s*['"]([^'"]+)['"]\s*]""")
        collectFiles(directory).forEach { file ->
            val text = runCatching { VfsUtilCore.loadText(file) }.getOrNull() ?: return@forEach
            pattern.findAll(text).mapTo(result) { it.groupValues[1] }
        }
        return result
    }

    private fun collectFiles(directory: VirtualFile): List<VirtualFile> {
        val result = mutableListOf<VirtualFile>()
        directory.children.forEach { child ->
            if (child.isDirectory) {
                result += collectFiles(child)
            } else if (child.extension.equals("php", ignoreCase = true)) {
                result += child
            }
        }
        return result
    }
}

/**
 * Один и тот же lang-ключ объявлен в разных файлах проекта.
 */
class DuplicateLangAcrossFilesInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PhpElementVisitor() {
            override fun visitPhpStringLiteralExpression(element: StringLiteralExpression) {
                if (!com.github.saundefined.bitrix_idea.php.BitrixLangSearch.isLangDefinition(element)) return
                val entries = com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
                    .findLangEntries(element.project, element.contents)
                val files = entries.map { it.file.path }.distinct()
                if (files.size <= 1) return
                holder.registerProblem(
                    element,
                    message("inspection.lang.duplicate.files.message", element.contents, files.size),
                    ProblemHighlightType.WEAK_WARNING,
                )
            }
        }
    }
}

/**
 * Обработчик события из `.settings.php` не найден в индексе PHP.
 */
class MissingEventHandlerInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PhpElementVisitor() {
            override fun visitPhpFile(file: PhpFile) {
                val virtualFile = file.virtualFile ?: return
                if (virtualFile.name != ".settings.php") return
                val text = runCatching { VfsUtilCore.loadText(virtualFile) }.getOrNull() ?: return
                val index = PhpIndex.getInstance(file.project)
                CALLABLE.findAll(text).forEach { match ->
                    val className = match.groupValues[1].removePrefix("\\")
                    if (className.isBlank()) return@forEach
                    if (index.getClassesByFQN("\\$className").isNotEmpty()) return@forEach
                    if (BitrixClassResolver.resolveClass(file.project, className) != null) return@forEach
                    val target = file.findElementAt(match.groups[1]?.range?.first ?: 0) ?: file
                    holder.registerProblem(
                        target,
                        message("inspection.event.handler.missing.message", className),
                        ProblemHighlightType.WEAK_WARNING,
                    )
                }
            }
        }
    }

    private companion object {
        val CALLABLE = Regex("""['"]\\?([A-Za-z_][A-Za-z0-9_\\]*)['"]\s*,\s*['"][A-Za-z_][A-Za-z0-9_]*['"]""")
    }
}
