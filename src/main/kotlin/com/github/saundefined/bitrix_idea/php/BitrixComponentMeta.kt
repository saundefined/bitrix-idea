package com.github.saundefined.bitrix_idea.php

import com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.ArrayAccessExpression
import com.jetbrains.php.lang.psi.elements.ArrayHashElement
import com.jetbrains.php.lang.psi.elements.AssignmentExpression
import com.jetbrains.php.lang.psi.elements.FunctionReference
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.php.lang.psi.elements.Variable

/**
 * Описание параметра компонента из `.parameters.php`.
 */
data class BitrixComponentParameter(
    val key: String,
    val name: String?,
    val description: String?,
    val target: PsiElement,
    val type: String? = null,
    val defaultValue: String? = null,
    val parent: String? = null,
    val required: Boolean = false,
)

/**
 * Метаданные компонента: `.description.php` и `.parameters.php`.
 * Все методы терпимы к отсутствующим или нестандартным файлам.
 */
object BitrixComponentMeta {
    private val KEY = Key.create<CachedValue<ComponentMeta>>("BitrixComponentMeta")
    private val STRING_ENTRY = Regex("""['"](NAME|DESCRIPTION|TITLE)['"]\s*=>\s*(.+?)(?:,\s*\n|,\s*$)""")
    private val LANG_CALL = Regex("""(?:Loc::getMessage|GetMessage)\s*\(\s*['"]([^'"]+)['"]""")
    private val LITERAL = Regex("""^\s*['"]([^'"]*)['"]""")

    private data class ComponentMeta(
        val description: Map<String, String?>,
        val parameters: Map<String, BitrixComponentParameter>,
    )

    fun componentIdForFile(file: VirtualFile): String? {
        val directory = componentDirectoryFor(file) ?: return null
        val vendor = directory.parent?.name ?: return null
        if (directory.parent?.parent?.name != "components") return null
        return "$vendor:${directory.name}"
    }

    fun componentDirectoryFor(file: VirtualFile): VirtualFile? {
        var current: VirtualFile? = if (file.isDirectory) file else file.parent
        while (current != null) {
            val parent = current.parent ?: return null
            if (parent.parent?.name == "components") return current
            if (current.name == "local" || current.name == "bitrix") return null
            current = parent
        }
        return null
    }

    /**
     * Если литерал используется как `$arParams['KEY']`, возвращает ID компонента и описание параметра.
     */
    fun parameterForLiteral(literal: StringLiteralExpression): Pair<String, BitrixComponentParameter>? {
        val componentId = componentIdForArrayAccess(literal) ?: return null
        val parameter = parameter(literal.project, componentId, literal.contents) ?: return null
        return componentId to parameter
    }

    /**
     * ID компонента для литерала в `$arParams['KEY']`, даже если ключа ещё нет.
     * Нужен автодополнению, где текущий ключ может быть неполным.
     */
    fun componentIdForArrayAccess(literal: StringLiteralExpression): String? {
        val arrayAccess = PsiTreeUtil.getParentOfType(literal, ArrayAccessExpression::class.java, false) ?: return null
        if (arrayAccess.index?.value !== literal) return null
        val variable = arrayAccess.value as? Variable ?: return null
        if (variable.name !in setOf("arParams", "arCurrentValues", "arValues")) return null
        val file = literal.containingFile.virtualFile ?: return null
        return componentIdForFile(file)
    }

    fun description(project: Project, componentId: String): Pair<String?, String?>? {
        val meta = load(project, componentId) ?: return null
        return meta.description["NAME"] to meta.description["DESCRIPTION"]
    }

    fun parameter(project: Project, componentId: String, key: String): BitrixComponentParameter? {
        return load(project, componentId)?.parameters?.get(key)
    }

    fun parameters(project: Project, componentId: String): Map<String, BitrixComponentParameter> {
        return load(project, componentId)?.parameters.orEmpty()
    }

    private fun load(project: Project, componentId: String): ComponentMeta? {
        val component = BitrixProjectStructure.getModel(project).findComponent(componentId) ?: return null
        val anchor = listOf("class.php", "component.php", ".description.php", ".parameters.php")
            .firstNotNullOfOrNull { component.directory.findChild(it) }
            ?: return null
        val psiFile = PsiManager.getInstance(project).findFile(anchor) ?: return null
        return CachedValuesManager.getCachedValue(psiFile, KEY, CachedValueProvider {
            CachedValueProvider.Result.create(
                ComponentMeta(
                    description = parseDescription(project, component.directory),
                    parameters = parseParameters(project, component.directory),
                ),
                psiFile,
            )
        })
    }

    private fun parseDescription(project: Project, directory: VirtualFile): Map<String, String?> {
        val file = directory.findChild(".description.php") ?: return emptyMap()
        val text = runCatching { VfsUtilCore.loadText(file) }.getOrNull() ?: return emptyMap()
        return STRING_ENTRY.findAll(text).associate { match ->
            val key = match.groupValues[1]
            key to resolveText(project, match.groupValues[2].trim())
        }
    }

    private fun parseParameters(project: Project, directory: VirtualFile): Map<String, BitrixComponentParameter> {
        val file = directory.findChild(".parameters.php") ?: return emptyMap()
        val psiFile = PsiManager.getInstance(project).findFile(file) ?: return emptyMap()
        val assignment = PsiTreeUtil.findChildrenOfType(psiFile, AssignmentExpression::class.java)
            .firstOrNull { it.variable?.text.orEmpty().contains("arComponentParameters") }
            ?: return emptyMap()
        val root = PsiTreeUtil.findChildrenOfType(assignment, ArrayHashElement::class.java)
        val parameters = root.firstOrNull {
            (it.key as? StringLiteralExpression)?.contents == "PARAMETERS"
        } ?: return emptyMap()

        val result = linkedMapOf<String, BitrixComponentParameter>()
        PsiTreeUtil.findChildrenOfType(parameters, ArrayHashElement::class.java).forEach { element ->
            val keyElement = element.key as? StringLiteralExpression ?: return@forEach
            val key = keyElement.contents
            if (key.isBlank() || result.containsKey(key)) return@forEach
            val value = element.value as? com.jetbrains.php.lang.psi.elements.ArrayCreationExpression ?: return@forEach
            var name: String? = null
            var description: String? = null
            var type: String? = null
            var defaultValue: String? = null
            var parent: String? = null
            var required = false
            value.hashElements.forEach { field ->
                when ((field.key as? StringLiteralExpression)?.contents) {
                    "NAME" -> name = resolveElement(project, field)
                    "DESCRIPTION" -> description = resolveElement(project, field)
                    "TYPE" -> type = resolveElement(project, field)
                    "DEFAULT" -> defaultValue = resolveElement(project, field)
                    "PARENT" -> parent = resolveElement(project, field)
                    "REQUIRED" -> required = resolveElement(project, field)?.uppercase() == "Y"
                }
            }
            result[key] = BitrixComponentParameter(
                key = key,
                name = name,
                description = description,
                target = keyElement,
                type = type,
                defaultValue = defaultValue,
                parent = parent,
                required = required,
            )
        }
        return result
    }

    private fun resolveElement(project: Project, element: ArrayHashElement): String? {
        val value = element.value
        if (value is StringLiteralExpression) return value.contents
        val call = PsiTreeUtil.findChildOfType(value, FunctionReference::class.java)
        if (call != null && BitrixPhp.isGetMessage(call)) {
            val key = (call.parameterList?.getParameter(0) as? StringLiteralExpression)?.contents
            if (!key.isNullOrBlank()) {
                return BitrixLangMessages.value(project, key) ?: key
            }
        }
        return value?.text?.trim()?.take(120)
    }

    private fun resolveText(project: Project, text: String): String? {
        LANG_CALL.find(text)?.groupValues?.getOrNull(1)?.let { key ->
            return BitrixLangMessages.value(project, key) ?: key
        }
        return LITERAL.find(text)?.groupValues?.getOrNull(1)
    }
}
