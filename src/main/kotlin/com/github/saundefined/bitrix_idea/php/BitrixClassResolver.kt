package com.github.saundefined.bitrix_idea.php

import com.intellij.openapi.project.Project
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass

object BitrixClassResolver {
    fun resolveClass(project: Project, value: String): PhpClass? {
        val name = value.trim().removePrefix("\\").substringBefore("::").trim()
        if (name.isEmpty()) return null
        val index = PhpIndex.getInstance(project)
        return index.getClassesByFQN("\\$name").firstOrNull()
            ?: index.getClassesByName(name.substringAfterLast('\\')).firstOrNull()
    }

    fun resolveMethod(project: Project, className: String, methodName: String): Method? {
        val phpClass = resolveClass(project, className) ?: return null
        return phpClass.findMethodByName(methodName)
    }

    fun parseCallable(value: String): Pair<String, String?> {
        val cleaned = value.trim().removeSuffix(";").removeSuffix("()").trim()
        val separator = cleaned.indexOf("::")
        if (separator < 0) return cleaned to null
        return cleaned.substring(0, separator).trim() to cleaned.substring(separator + 2).trim()
    }
}
