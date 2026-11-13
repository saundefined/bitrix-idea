package com.github.saundefined.bitrix_idea.php.inspections

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.php.BitrixComponentParameters
import com.github.saundefined.bitrix_idea.php.BitrixCoreModules
import com.github.saundefined.bitrix_idea.php.BitrixLangSearch
import com.github.saundefined.bitrix_idea.php.BitrixLangUsageIndex
import com.github.saundefined.bitrix_idea.php.BitrixPhp
import com.github.saundefined.bitrix_idea.settings.AppSettingsState
import com.github.saundefined.bitrix_idea.settings.BitrixProjectSettings
import com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
import com.intellij.codeInspection.LocalInspectionTool
import com.jetbrains.php.PhpIndex
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.ArrayAccessExpression
import com.jetbrains.php.lang.psi.elements.ArrayHashElement
import com.jetbrains.php.lang.psi.elements.FunctionReference
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.ParameterList
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.php.lang.psi.visitors.PhpElementVisitor

class MissingLangMessageInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PhpElementVisitor() {
            override fun visitPhpStringLiteralExpression(element: StringLiteralExpression) {
                val call = BitrixPhp.directCall(element) ?: return
                if (!BitrixPhp.isGetMessage(call) || BitrixPhp.argumentIndex(element) != 0) return

                val key = element.contents
                if (key.isBlank() ||
                    BitrixCoreModules.isLikelyCoreKey(key) ||
                    BitrixProjectStructure.hasLangKey(element.project, key)
                ) {
                    return
                }

                holder.registerProblem(
                    element,
                    message("inspection.missing.lang.message", key),
                    ProblemHighlightType.WEAK_WARNING,
                    AddLangKeyFix(element.containingFile, key, defaultLanguage()),
                )
            }
        }
    }

    private fun defaultLanguage(): String = AppSettingsState.getInstance().languages.firstOrNull() ?: "ru"
}

class UnknownModuleInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PhpElementVisitor() {
            override fun visitPhpStringLiteralExpression(element: StringLiteralExpression) {
                val call = BitrixPhp.directCall(element) ?: return
                if (!BitrixPhp.isIncludeModule(call) || BitrixPhp.argumentIndex(element) != 0) return

                val moduleId = element.contents
                if (moduleId.isBlank()) return
                val model = BitrixProjectStructure.getModel(element.project)
                val disabled = BitrixProjectSettings.getInstance(element.project).disabledModules
                if (model.findModule(moduleId) != null || moduleId in BitrixCoreModules.allIds() || moduleId in disabled) return

                holder.registerProblem(
                    element,
                    message("inspection.unknown.module.message", moduleId),
                    ProblemHighlightType.WEAK_WARNING,
                )
            }
        }
    }
}

class UnknownComponentInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PhpElementVisitor() {
            override fun visitPhpStringLiteralExpression(element: StringLiteralExpression) {
                val call = BitrixPhp.directCall(element) ?: return
                if (!BitrixPhp.isIncludeComponent(call) || BitrixPhp.argumentIndex(element) != 0) return

                val componentId = element.contents
                if (componentId.isBlank()) return
                val component = BitrixProjectStructure.getModel(element.project).findComponent(componentId)
                if (component == null) {
                    holder.registerProblem(
                        element,
                        message("inspection.unknown.component.message", componentId),
                        ProblemHighlightType.WEAK_WARNING,
                    )
                } else if (component.entryFiles().isEmpty()) {
                    holder.registerProblem(
                        element,
                        message("inspection.component.entry.missing.message", componentId),
                        ProblemHighlightType.WEAK_WARNING,
                    )
                }
            }
        }
    }
}

class MissingIncludeGuardInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PhpElementVisitor() {
            override fun visitPhpFile(file: PhpFile) {
                val virtualFile = file.virtualFile ?: return
                val path = virtualFile.path
                if (!path.contains("/components/") && !path.contains("/modules/")) return
                if (path.contains("/lang/") || path.contains("/install/")) return
                if (virtualFile.name !in GUARD_FILES) return

                val text = runCatching { VfsUtilCore.loadText(virtualFile) }.getOrNull() ?: return
                if (text.contains("B_PROLOG_INCLUDED")) return

                holder.registerProblem(
                    file,
                    message("inspection.guard.message"),
                    ProblemHighlightType.WEAK_WARNING,
                    AddIncludeGuardFix(),
                )
            }
        }
    }

    private companion object {
        val GUARD_FILES = setOf(
            "class.php",
            "component.php",
            "template.php",
            ".description.php",
            ".parameters.php",
            "result_modifier.php",
            "component_epilog.php",
        )
    }
}

class UnusedLangMessageInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PhpElementVisitor() {
            override fun visitPhpFile(file: PhpFile) {
                val virtualFile = file.virtualFile ?: return
                if (!virtualFile.path.contains("/local/") || !virtualFile.path.contains("/lang/")) return
                val used = BitrixLangUsageIndex.usedKeys(file.project)
                PsiTreeUtil.findChildrenOfType(file, StringLiteralExpression::class.java).forEach { literal ->
                    if (!BitrixLangSearch.isLangDefinition(literal)) return@forEach
                    val key = literal.contents
                    if (key.isNotBlank() && key !in used) {
                        holder.registerProblem(
                            literal,
                            message("inspection.lang.unused.message", key),
                            ProblemHighlightType.WEAK_WARNING,
                        )
                    }
                }
            }
        }
    }
}

class DuplicateLangMessageInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PhpElementVisitor() {
            override fun visitPhpFile(file: PhpFile) {
                val seen = mutableSetOf<String>()
                PsiTreeUtil.findChildrenOfType(file, StringLiteralExpression::class.java).forEach { literal ->
                    if (!BitrixLangSearch.isLangDefinition(literal)) return@forEach
                    val key = literal.contents
                    if (key.isBlank()) return@forEach
                    if (!seen.add(key)) {
                        holder.registerProblem(
                            literal,
                            message("inspection.lang.duplicate.message", key),
                            ProblemHighlightType.WEAK_WARNING,
                        )
                    }
                }
            }
        }
    }
}

class MissingComponentTemplateInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PhpElementVisitor() {
            override fun visitPhpFile(file: PhpFile) {
                val virtualFile = file.virtualFile ?: return
                if (!virtualFile.path.contains("/components/") || virtualFile.name != "class.php") return
                val templates = virtualFile.parent?.findChild("templates") ?: return
                templates.children.filter { it.isDirectory }.forEach { template ->
                    if (template.findChild("template.php") == null) {
                        holder.registerProblem(
                            file,
                            message("inspection.component.template.missing.message", template.name),
                            ProblemHighlightType.WEAK_WARNING,
                            CreateComponentTemplateFileFix(template),
                        )
                    }
                }
            }
        }
    }
}

class MissingComponentLangInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PhpElementVisitor() {
            override fun visitPhpFile(file: PhpFile) {
                val virtualFile = file.virtualFile ?: return
                if (!virtualFile.path.contains("/components/")) return
                if (virtualFile.name !in setOf("class.php", ".description.php", ".parameters.php")) return
                if (virtualFile.parent?.findChild("lang") != null) return
                val text = runCatching { VfsUtilCore.loadText(virtualFile) }.getOrNull() ?: return
                if (!text.contains("Loc::getMessage") && !text.contains("GetMessage")) return
                holder.registerProblem(
                    file,
                    message("inspection.component.lang.missing.message"),
                    ProblemHighlightType.WEAK_WARNING,
                )
            }
        }
    }
}

class MissingSettingsClassInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PhpElementVisitor() {
            override fun visitPhpFile(file: PhpFile) {
                val virtualFile = file.virtualFile ?: return
                if (virtualFile.name != ".settings.php") return
                val text = runCatching { VfsUtilCore.loadText(virtualFile) }.getOrNull() ?: return
                val index = PhpIndex.getInstance(file.project)
                CLASS_REFERENCE.findAll(text).forEach { match ->
                    val fqn = match.groupValues[1]
                    if (index.getClassesByFQN("\\$fqn").isEmpty()) {
                        holder.registerProblem(
                            file,
                            message("inspection.settings.class.missing.message", fqn),
                            ProblemHighlightType.WEAK_WARNING,
                        )
                    }
                }
            }
        }
    }

    private companion object {
        val CLASS_REFERENCE = Regex("""(?:\\?)([A-Za-z_][A-Za-z0-9_]*\\[A-Za-z0-9_\\]+)::class""")
    }
}

class UnknownComponentParameterInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PhpElementVisitor() {
            override fun visitPhpArrayHashElement(element: ArrayHashElement) {
                val keyLiteral = element.key as? StringLiteralExpression ?: return

                val parameterList = PsiTreeUtil.getParentOfType(element, ParameterList::class.java, false) ?: return
                val call = PsiTreeUtil.getParentOfType(parameterList, FunctionReference::class.java, true) ?: return
                if (call.parameterList !== parameterList || !BitrixPhp.isIncludeComponent(call)) return

                val params = parameterList.parameters.getOrNull(2) ?: return
                if (!PsiTreeUtil.isAncestor(params, element, false)) return

                val componentLiteral = parameterList.parameters.getOrNull(0) as? StringLiteralExpression ?: return
                val key = keyLiteral.contents
                if (key.isBlank()) return

                val known = BitrixComponentParameters.keys(element.project, componentLiteral.contents) ?: return
                if (key in known) return

                holder.registerProblem(
                    keyLiteral,
                    message("inspection.component.parameter.message", key),
                    ProblemHighlightType.WEAK_WARNING,
                )
            }
        }
    }
}

class DeprecatedApiInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PhpElementVisitor() {
            override fun visitPhpMethodReference(element: MethodReference) {
                val version = BitrixProjectStructure.bitrixVersion(element.project)
                if (versionAtLeast(version, "14.0.0") && BitrixPhp.isCModuleInclude(element)) {
                    holder.registerProblem(
                        element,
                        message("inspection.deprecated.cmodule.message"),
                        ProblemHighlightType.WEAK_WARNING,
                        ReplaceWithLoaderFix(),
                    )
                    return
                }
                val owner = BitrixPhp.className(element).substringAfterLast('\\')
                val method: String = element.name ?: return
                val rule: LegacyRule = LEGACY_RULES.firstOrNull { it.owner == owner && it.method == method } ?: return
                if (!versionAtLeast(version, rule.since)) return
                holder.registerProblem(
                    element as com.intellij.psi.PsiElement,
                    message("inspection.deprecated.api.message", "$owner::$method", rule.replacement),
                    ProblemHighlightType.WEAK_WARNING,
                )
            }

            override fun visitPhpFunctionCall(element: FunctionReference) {
                if (BitrixPhp.isLegacyGetMessage(element) &&
                    versionAtLeast(BitrixProjectStructure.bitrixVersion(element.project), "18.0.0")
                ) {
                    holder.registerProblem(
                        element,
                        message("inspection.deprecated.getmessage.message"),
                        ProblemHighlightType.WEAK_WARNING,
                        ReplaceGlobalGetMessageFix(),
                    )
                }
            }
        }
    }

    private fun versionAtLeast(current: String?, required: String): Boolean {
        if (current == null) return true
        val currentParts = current.split('.').mapNotNull { it.toIntOrNull() }
        val requiredParts = required.split('.').mapNotNull { it.toIntOrNull() }
        for (index in 0 until maxOf(currentParts.size, requiredParts.size)) {
            val left = currentParts.getOrElse(index) { 0 }
            val right = requiredParts.getOrElse(index) { 0 }
            if (left != right) return left > right
        }
        return true
    }

    private data class LegacyRule(
        val owner: String,
        val method: String,
        val since: String,
        val replacement: String,
    )

    private companion object {
        val LEGACY_RULES = listOf(
            LegacyRule("CSite", "GetList", "20.0.0", "\\Bitrix\\Main\\SiteTable::getList()"),
            LegacyRule("CUser", "GetList", "20.0.0", "\\Bitrix\\Main\\UserTable::getList()"),
            LegacyRule("CIBlock", "GetList", "20.0.0", "\\Bitrix\\Iblock\\IblockTable::getList()"),
            LegacyRule("CIBlockElement", "GetList", "20.0.0", "\\Bitrix\\Iblock\\ElementTable::getList()"),
        )
    }
}

class LangSyncInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PhpElementVisitor() {
            override fun visitPhpFile(file: PhpFile) {
                val virtualFile = file.virtualFile ?: return
                val language = currentLanguage(virtualFile.path) ?: return
                val targetLanguage = when (language) {
                    "ru" -> "en"
                    "en" -> "ru"
                    else -> return
                }
                val companion = BitrixProjectStructure.companionLangFile(virtualFile, targetLanguage) ?: return
                val companionText = runCatching { VfsUtilCore.loadText(companion) }.getOrNull() ?: return

                PsiTreeUtil.findChildrenOfType(file, StringLiteralExpression::class.java).forEach { literal ->
                    val arrayAccess = PsiTreeUtil.getParentOfType(literal, ArrayAccessExpression::class.java, false)
                        ?: return@forEach
                    if (arrayAccess.index?.value !== literal) return@forEach
                    if (!arrayAccess.value?.text.orEmpty().contains("\$MESS")) return@forEach

                    val key = literal.contents
                    if (key.isBlank()) return@forEach
                    if (companionText.contains("['$key']") || companionText.contains("[\"$key\"]")) return@forEach

                    holder.registerProblem(
                        literal,
                        message("inspection.lang.sync.message", key, targetLanguage),
                        ProblemHighlightType.WEAK_WARNING,
                        AddLangKeyFix(file, key, targetLanguage),
                    )
                }
            }
        }
    }

    private fun currentLanguage(path: String): String? {
        val marker = "/lang/"
        val start = path.indexOf(marker)
        if (start < 0) return null
        val end = path.indexOf('/', start + marker.length)
        if (end < 0) return null
        return path.substring(start + marker.length, end)
    }
}
