package com.github.saundefined.bitrix_idea.php

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.FunctionReference
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.ParameterList
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

/**
 * Небольшие помощники для работы с вызовами Bitrix без опоры на разрешённые PHP-типы.
 * Все проверки построены на именах, поэтому работают быстро и терпимы к неполным проектам.
 */
object BitrixPhp {
    fun literal(element: PsiElement?): StringLiteralExpression? {
        return when (element) {
            is StringLiteralExpression -> element
            else -> element?.let { PsiTreeUtil.getParentOfType(it, StringLiteralExpression::class.java, false) }
        }
    }

    fun directCall(literal: StringLiteralExpression): FunctionReference? {
        val parameterList = PsiTreeUtil.getParentOfType(literal, ParameterList::class.java, false) ?: return null
        if (parameterList.parameters.none { it === literal }) return null
        val call = PsiTreeUtil.getParentOfType(parameterList, FunctionReference::class.java, true) ?: return null
        return call.takeIf { it.parameterList === parameterList }
    }

    fun argumentIndex(literal: StringLiteralExpression): Int? {
        val parameterList = PsiTreeUtil.getParentOfType(literal, ParameterList::class.java, false) ?: return null
        val index = parameterList.parameters.indexOfFirst { it === literal }
        return index.takeIf { it >= 0 }
    }

    fun className(call: FunctionReference): String {
        val reference = call as? MethodReference ?: return ""
        return reference.classReference?.text?.trim()?.trimStart('\\').orEmpty()
    }

    fun isClass(call: FunctionReference, suffix: String): Boolean = className(call).endsWith(suffix)

    fun isIncludeComponent(call: FunctionReference): Boolean = call.name == "IncludeComponent"

    fun isIncludeModule(call: FunctionReference): Boolean {
        val name = call.name ?: return false
        if (name !in setOf("includeModule", "requireModule", "includeSharewareModule", "IncludeModule", "IncludeModuleEx")) {
            return false
        }
        return isClass(call, "Loader") || isClass(call, "CModule")
    }

    fun isGetMessage(call: FunctionReference): Boolean {
        val name = call.name ?: return false
        if (name == "GetMessage") return call !is MethodReference
        if (name == "getMessage") return call is MethodReference && isClass(call, "Loc")
        return false
    }

    fun isEventRegistration(call: FunctionReference): Boolean {
        val name = call.name ?: return false
        if (name in setOf("registerEventHandler", "registerEventHandlerCompatible")) {
            return isClass(call, "EventManager")
        }
        if (name == "addEventHandler" || name == "AddEventHandler") {
            return call !is MethodReference || isClass(call, "EventManager")
        }
        return false
    }

    fun isAgentCall(call: FunctionReference): Boolean {
        return call.name == "AddAgent" && isClass(call, "CAgent")
    }

    fun isCModuleInclude(call: FunctionReference): Boolean {
        return call.name == "IncludeModule" && isClass(call, "CModule")
    }

    fun isLegacyGetMessage(call: FunctionReference): Boolean {
        return call.name == "GetMessage" && call !is MethodReference
    }
}
