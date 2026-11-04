package com.github.saundefined.bitrix_idea.php

import com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.psi.elements.FunctionReference
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class BitrixReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression::class.java),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    context: ProcessingContext,
                ): Array<PsiReference> {
                    val literal = element as? StringLiteralExpression ?: return PsiReference.EMPTY_ARRAY
                    val call = BitrixPhp.directCall(literal) ?: return PsiReference.EMPTY_ARRAY
                    val index = BitrixPhp.argumentIndex(literal) ?: return PsiReference.EMPTY_ARRAY
                    val targets = BitrixTargets.resolve(literal, call, index)
                    if (targets.isEmpty()) return PsiReference.EMPTY_ARRAY
                    return arrayOf(BitrixPolyReference(literal, targets))
                }
            },
            PsiReferenceRegistrar.DEFAULT_PRIORITY,
        )
    }
}

private class BitrixPolyReference(
    element: PsiElement,
    private val targets: Array<PsiElement>,
) : PsiPolyVariantReferenceBase<PsiElement>(
    element,
    (element as StringLiteralExpression).valueRange,
    true,
) {
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        return targets.map { PsiElementResolveResult(it) }.toTypedArray()
    }

    override fun getVariants(): Array<Any> = emptyArray()
}

object BitrixTargets {
    fun resolve(
        literal: StringLiteralExpression,
        call: FunctionReference,
        index: Int,
    ): Array<PsiElement> {
        val project = literal.project
        val value = literal.contents
        val templateArgument = BitrixPhp.isIncludeComponent(call) && index == 1
        if (value.isBlank() && !templateArgument) return emptyArray()

        return when {
            BitrixPhp.isIncludeComponent(call) && index == 0 -> componentTargets(project, value)
            BitrixPhp.isIncludeComponent(call) && index == 1 -> templateTargets(project, call, value)
            BitrixPhp.isIncludeModule(call) && index == 0 -> moduleTargets(project, value)
            BitrixPhp.isGetMessage(call) && index == 0 -> langTargets(project, value)
            BitrixPhp.isEventRegistration(call) -> eventTargets(project, call, index, value)
            BitrixPhp.isAgentCall(call) && index == 0 -> callableTargets(project, value)
            else -> emptyArray()
        }
    }

    private fun componentTargets(project: Project, componentId: String): Array<PsiElement> {
        val component = BitrixProjectStructure.getModel(project).findComponent(componentId) ?: return emptyArray()
        val files = component.entryFiles().mapNotNull { psiFile(project, it) }
        if (files.isNotEmpty()) return files.toTypedArray()
        return listOfNotNull(psiDirectory(project, component.directory)).toTypedArray()
    }

    private fun templateTargets(project: Project, call: FunctionReference, templateName: String): Array<PsiElement> {
        val componentLiteral = call.parameterList?.getParameter(0) as? StringLiteralExpression ?: return emptyArray()
        val componentId = componentLiteral.contents
        if (componentId.isBlank()) return emptyArray()

        val dirs = BitrixProjectStructure.findComponentTemplateDirs(project, componentId, templateName)
        val templateFile = dirs.firstNotNullOfOrNull { dir ->
            dir.findChild("template.php")?.let { psiFile(project, it) }
        }
        if (templateFile != null) return arrayOf(templateFile)

        val directory = dirs.firstNotNullOfOrNull { psiDirectory(project, it) }
        return if (directory != null) arrayOf(directory) else emptyArray()
    }

    private fun eventTargets(
        project: Project,
        call: FunctionReference,
        index: Int,
        value: String,
    ): Array<PsiElement> {
        val name = call.name.orEmpty()
        if (name == "registerEventHandler" || name == "registerEventHandlerCompatible") {
            return when (index) {
                3 -> classTargets(project, value)
                4 -> {
                    val classLiteral = call.parameterList?.getParameter(3) as? StringLiteralExpression
                    if (classLiteral == null) emptyArray()
                    else methodTargets(project, classLiteral.contents, value)
                }
                else -> emptyArray()
            }
        }
        if (index == 2) {
            val (className, methodName) = BitrixClassResolver.parseCallable(value)
            return if (methodName.isNullOrBlank()) {
                classTargets(project, className)
            } else {
                methodTargets(project, className, methodName)
            }
        }
        return emptyArray()
    }

    private fun callableTargets(project: Project, value: String): Array<PsiElement> {
        val (className, methodName) = BitrixClassResolver.parseCallable(value)
        return if (methodName.isNullOrBlank()) {
            classTargets(project, className)
        } else {
            methodTargets(project, className, methodName)
        }
    }

    private fun classTargets(project: Project, value: String): Array<PsiElement> {
        val phpClass = BitrixClassResolver.resolveClass(project, value) ?: return emptyArray()
        return arrayOf(phpClass)
    }

    private fun methodTargets(project: Project, className: String, methodName: String): Array<PsiElement> {
        val method = BitrixClassResolver.resolveMethod(project, className, methodName)
        return if (method != null) arrayOf(method) else classTargets(project, className)
    }

    private fun moduleTargets(project: Project, moduleId: String): Array<PsiElement> {
        val module = BitrixProjectStructure.getModel(project).findModule(moduleId) ?: return emptyArray()
        val candidates = listOf("include.php", ".settings.php", "install/version.php", "install/index.php")
        val files = candidates.mapNotNull { module.directory.findFileByRelativePath(it) }
            .mapNotNull { psiFile(project, it) }
        if (files.isNotEmpty()) return files.toTypedArray()
        return listOfNotNull(psiDirectory(project, module.directory)).toTypedArray()
    }

    private fun langTargets(project: Project, key: String): Array<PsiElement> {
        return BitrixProjectStructure.findLangEntries(project, key)
            .mapNotNull { BitrixProjectStructure.psiElementAt(project, it) }
            .distinctBy { it.toString() }
            .toTypedArray()
    }

    fun psiFile(project: Project, file: VirtualFile): PsiFile? =
        PsiManager.getInstance(project).findFile(file)

    fun psiDirectory(project: Project, directory: VirtualFile): PsiDirectory? =
        PsiManager.getInstance(project).findDirectory(directory)
}
