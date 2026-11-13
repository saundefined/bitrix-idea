package com.github.saundefined.bitrix_idea.php

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.php.lang.psi.elements.FunctionReference
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class BitrixDocumentationProvider : AbstractDocumentationProvider() {
    override fun generateDoc(element: PsiElement, originalElement: PsiElement?): String? {
        if (element is PsiFile) return settingsFileDoc(element)

        val literal = element as? StringLiteralExpression ?: return null

        BitrixComponentMeta.parameterForLiteral(literal)?.let { (componentId, parameter) ->
            return parameterDoc(componentId, parameter)
        }

        if (literal.containingFile.virtualFile?.name == ".settings.php") {
            settingsLiteralDoc(literal)?.let { return it }
        }

        val call = BitrixPhp.directCall(literal) ?: return null
        val index = BitrixPhp.argumentIndex(literal) ?: return null

        return when {
            BitrixPhp.isGetMessage(call) && index == 0 -> langDoc(literal.project, literal.contents)
            BitrixPhp.isIncludeComponent(call) && index == 0 -> componentDoc(literal.project, literal.contents)
            BitrixPhp.isIncludeComponent(call) && index == 1 -> templateDoc(literal.project, call, literal.contents)
            BitrixPhp.isIncludeModule(call) && index == 0 -> moduleDoc(literal.project, literal.contents)
            else -> null
        }
    }

    private fun langDoc(project: Project, key: String): String {
        val translations = BitrixLangMessages.translations(project, key)
        if (translations.isEmpty()) return "<b>$key</b><br>${message("documentation.not.found")}"
        return buildString {
            append("<b>$key</b>")
            translations.forEach { (language, value) ->
                append("<br><b>$language:</b> $value")
            }
        }
    }

    private fun parameterDoc(componentId: String, parameter: BitrixComponentParameter): String {
        return buildString {
            append("<b>${escape(parameter.key)}</b>")
            parameter.name?.let { append("<br><b>${message("documentation.parameter.name")}:</b> ${escape(it)}") }
            parameter.description?.let { append("<br>${escape(it)}") }
            append("<br><b>${message("documentation.component")}:</b> ${escape(componentId)}")
        }
    }

    private fun componentDoc(project: Project, componentId: String): String {
        val component = BitrixProjectStructure.getModel(project).findComponent(componentId)
        val path = component?.directory?.path ?: message("documentation.not.found")
        val description = BitrixComponentMeta.description(project, componentId)

        val templates = BitrixProjectStructure.availableTemplateNames(project, componentId)
            .sorted()
            .joinToString(", ")

        return buildString {
            append("<b>${escape(componentId)}</b>")
            description?.first?.let { append("<br><b>${message("documentation.component.name")}:</b> ${escape(it)}") }
            description?.second?.let { append("<br>${escape(it)}") }
            append("<br><b>${message("documentation.path")}:</b> ${escape(path)}")
            if (templates.isNotEmpty()) {
                append("<br><b>${message("documentation.templates")}:</b> ${escape(templates)}")
            }
            if (component != null && !component.local) {
                append("<br><i>${message("documentation.component.override.hint")}</i>")
            }
        }
    }

    private fun templateDoc(project: Project, call: FunctionReference, templateName: String): String {
        val componentLiteral = call.parameterList?.getParameter(0) as? StringLiteralExpression ?: return ""
        val template = templateName.ifBlank { ".default" }
        val dirs = BitrixProjectStructure.findComponentTemplateDirs(project, componentLiteral.contents, template)
        val active = dirs.firstOrNull()
        val paths = if (dirs.isEmpty()) {
            message("documentation.not.found")
        } else {
            dirs.joinToString("<br>") { dir ->
                val prefix = if (dir == active) "→ " else ""
                "$prefix${escape(dir.path)}"
            }
        }
        return "<b>${message("documentation.template")}:</b> ${escape(componentLiteral.contents)}:${escape(template)}<br>$paths"
    }

    private fun moduleDoc(project: Project, moduleId: String): String {
        val module = BitrixProjectStructure.getModel(project).findModule(moduleId)
        val path = module?.directory?.path ?: message("documentation.not.found")
        return "<b>${escape(moduleId)}</b><br><b>${message("documentation.path")}:</b> ${escape(path)}"
    }

    private fun settingsLiteralDoc(literal: StringLiteralExpression): String? {
        val value = literal.contents
        if (value.contains("::")) {
            return callableDoc(literal.project, value)
        }
        if (value.matches(Regex("^On[A-Za-z0-9_]+"))) {
            return buildString {
                append("<b>${message("documentation.event")}:</b> ${escape(value)}")
                append("<br>${message("documentation.event.registered")}")
            }
        }
        return null
    }

    private fun callableDoc(project: Project, value: String): String {
        val (className, methodName) = BitrixClassResolver.parseCallable(value)
        val phpClass = BitrixClassResolver.resolveClass(project, className)
        if (phpClass == null) {
            return "<b>${escape(value)}</b><br>${message("documentation.class.not.found")}"
        }
        return buildString {
            append("<b>${escape(phpClass.fqn)}</b>")
            methodName?.let { append("<br><b>${message("documentation.method")}:</b> ${escape(it)}") }
            phpClass.containingFile?.virtualFile?.path?.let {
                append("<br><b>${message("documentation.path")}:</b> ${escape(it)}")
            }
        }
    }

    private fun settingsFileDoc(file: PsiFile): String? {
        val virtualFile = file.virtualFile ?: return null
        if (virtualFile.name != ".settings.php") return null
        val text = runCatching { VfsUtilCore.loadText(virtualFile) }.getOrNull() ?: return null
        val classes = CLASS_REFERENCE.findAll(text)
            .map { it.groupValues[1] }
            .toSet()
            .sorted()

        return buildString {
            append("<b>.settings.php</b>")
            append("<br><b>${message("documentation.path")}:</b> ${escape(virtualFile.path)}")
            if (classes.isNotEmpty()) {
                append("<br><b>${message("documentation.settings.classes")}:</b>")
                classes.take(20).forEach { append("<br>${escape(it)}") }
            }
            if (text.contains("'events'") || text.contains("\"events\"")) {
                append("<br>${message("documentation.settings.events")}")
            }
        }
    }

    private fun escape(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }

    private companion object {
        val CLASS_REFERENCE = Regex("""(?:\\?)([A-Za-z_][A-Za-z0-9_]*\\[A-Za-z0-9_\\]+)::class""")
    }
}
