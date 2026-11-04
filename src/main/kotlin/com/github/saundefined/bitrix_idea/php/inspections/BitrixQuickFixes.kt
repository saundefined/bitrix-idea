package com.github.saundefined.bitrix_idea.php.inspections

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.php.lang.psi.elements.FunctionReference
import com.jetbrains.php.lang.psi.elements.MethodReference

class AddLangKeyFix(
    private val sourceFile: PsiFile,
    private val key: String,
    private val language: String,
) : LocalQuickFix {
    override fun getFamilyName(): String = message("inspection.fix.add.lang.family")

    override fun getName(): String = message("inspection.fix.add.lang.name", key, language)

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val source = sourceFile.virtualFile ?: return
        WriteCommandAction.runWriteCommandAction(project) {
            val target = ensureTarget(project, source, language) ?: return@runWriteCommandAction
            val existing = FileDocumentManager.getInstance().getDocument(target)?.text
                ?: runCatching { VfsUtilCore.loadText(target) }.getOrNull()
                ?: ""
            if (existing.contains("\$MESS['${escape(key)}']") || existing.contains("\$MESS[\"${escape(key)}\"]")) {
                return@runWriteCommandAction
            }
            val line = "\$MESS['${escape(key)}'] = '${escape(key)}';\n"
            val closeTag = existing.lastIndexOf("?>")
            val updated = if (closeTag >= 0) {
                existing.substring(0, closeTag) + line + existing.substring(closeTag)
            } else {
                existing.trimEnd() + "\n" + line
            }
            val document = FileDocumentManager.getInstance().getDocument(target)
            if (document != null) {
                document.setText(updated)
                FileDocumentManager.getInstance().saveDocument(document)
            } else {
                VfsUtil.saveText(target, updated)
            }
        }
    }

    private fun ensureTarget(project: Project, source: VirtualFile, language: String): VirtualFile? {
        if (source.path.contains("/lang/")) {
            BitrixProjectStructure.companionLangFile(source, language)?.let { return it }
            val langRoot = source.parent?.parent ?: return null
            var directory = langRoot.findChild(language) ?: langRoot.createChildDirectory(this, language)
            val relative = VfsUtilCore.getRelativePath(source, langRoot, '/') ?: return null
            relative.split('/').drop(1).dropLast(1).forEach { part ->
                directory = directory.findChild(part) ?: directory.createChildDirectory(this, part)
            }
            return directory.findChild(source.name) ?: directory.createChildData(this, source.name)
        }
        val base = BitrixProjectStructure.findOwnerBase(source) ?: source.parent ?: return null
        val relative = VfsUtilCore.getRelativePath(source, base, '/') ?: source.name
        var directory = base
        val parts = listOf("lang", language) + relative.split('/').dropLast(1)
        parts.forEach { part ->
            directory = directory.findChild(part) ?: directory.createChildDirectory(this, part)
        }
        val fileName = relative.substringAfterLast('/')
        return directory.findChild(fileName) ?: directory.createChildData(this, fileName)
    }

    private fun escape(value: String): String = value.replace("\\", "\\\\").replace("'", "\\'")
}

class AddIncludeGuardFix : LocalQuickFix {
    override fun getFamilyName(): String = message("inspection.fix.guard.family")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val file = descriptor.psiElement as? PsiFile ?: descriptor.psiElement.containingFile ?: return
        WriteCommandAction.runWriteCommandAction(project) {
            val document = PsiDocumentManager.getInstance(project).getDocument(file)
                ?: return@runWriteCommandAction
            val text = document.text
            if (text.contains("B_PROLOG_INCLUDED")) return@runWriteCommandAction
            val phpStart = text.indexOf("<?php")
            if (phpStart < 0) return@runWriteCommandAction
            document.insertString(
                phpStart + 5,
                "\n\nif (!defined('B_PROLOG_INCLUDED') || B_PROLOG_INCLUDED !== true) {\n    die();\n}\n",
            )
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }
    }
}

class ReplaceWithLoaderFix : LocalQuickFix {
    override fun getFamilyName(): String = message("inspection.fix.loader.family")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val call = descriptor.psiElement as? MethodReference ?: return
        val args = call.parameterList?.text ?: "()"
        replaceElement(project, call, "\\Bitrix\\Main\\Loader::includeModule$args")
    }
}

class ReplaceGlobalGetMessageFix : LocalQuickFix {
    override fun getFamilyName(): String = message("inspection.fix.loc.family")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val call = descriptor.psiElement as? FunctionReference ?: return
        val args = call.parameterList?.text ?: "()"
        replaceElement(project, call, "\\Bitrix\\Main\\Localization\\Loc::getMessage$args")
    }
}

private fun replaceElement(project: Project, element: PsiElement, replacement: String) {
    WriteCommandAction.runWriteCommandAction(project) {
        val document = PsiDocumentManager.getInstance(project)
            .getDocument(element.containingFile) ?: return@runWriteCommandAction
        document.replaceString(element.textRange.startOffset, element.textRange.endOffset, replacement)
        PsiDocumentManager.getInstance(project).commitDocument(document)
    }
}
