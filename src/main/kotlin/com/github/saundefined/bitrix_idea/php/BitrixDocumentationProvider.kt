package com.github.saundefined.bitrix_idea.php

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.psi.elements.FunctionReference
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class BitrixDocumentationProvider : AbstractDocumentationProvider() {
    override fun generateDoc(element: PsiElement, originalElement: PsiElement?): String? {
        val literal = element as? StringLiteralExpression ?: return null
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

    private fun componentDoc(project: Project, componentId: String): String {
        val component = BitrixProjectStructure.getModel(project).findComponent(componentId)
        val path = component?.directory?.path ?: message("documentation.not.found")
        return "<b>${message("documentation.component")}:</b> $componentId<br><b>${message("documentation.path")}:</b> $path"
    }

    private fun templateDoc(project: Project, call: FunctionReference, templateName: String): String {
        val componentLiteral = call.parameterList?.getParameter(0) as? StringLiteralExpression ?: return ""
        val template = templateName.ifBlank { ".default" }
        val dirs = BitrixProjectStructure.findComponentTemplateDirs(project, componentLiteral.contents, template)
        val paths = if (dirs.isEmpty()) {
            message("documentation.not.found")
        } else {
            dirs.joinToString("<br>") { it.path }
        }
        return "<b>${message("documentation.template")}:</b> ${componentLiteral.contents}:$template<br>$paths"
    }

    private fun moduleDoc(project: Project, moduleId: String): String {
        val module = BitrixProjectStructure.getModel(project).findModule(moduleId)
        val path = module?.directory?.path ?: message("documentation.not.found")
        return "<b>${message("documentation.module")}:</b> $moduleId<br><b>${message("documentation.path")}:</b> $path"
    }
}
