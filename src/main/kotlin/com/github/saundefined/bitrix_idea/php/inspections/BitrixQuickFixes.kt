package com.github.saundefined.bitrix_idea.php.inspections

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.settings.AppSettingsState
import com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
import com.github.saundefined.bitrix_idea.templates.component.ComponentParametersWriter
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

/**
 * Добавляет отсутствующий параметр в `.parameters.php` и lang-файлы компонента.
 */
class AddComponentParameterFix(
    private val componentDirectory: VirtualFile,
    private val key: String,
    private val parameterName: String,
) : LocalQuickFix {
    override fun getFamilyName(): String = message("inspection.fix.add.parameter.family")

    override fun getName(): String = message("inspection.fix.add.parameter.name", key)

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        WriteCommandAction.runWriteCommandAction(project) {
            val parametersFile = componentDirectory.findChild(".parameters.php") ?: return@runWriteCommandAction
            val text = runCatching { VfsUtilCore.loadText(parametersFile) }.getOrNull() ?: return@runWriteCommandAction
            val updated = addParameter(text, key, parameterName)
            if (updated != text) write(parametersFile, updated)

            val langRoot = componentDirectory.findChild("lang")
                ?: componentDirectory.createChildDirectory(this, "lang")
            val languages = AppSettingsState.getInstance().languages.ifEmpty { listOf("ru") }
            languages.forEach { language ->
                val langDir = langRoot.findChild(language) ?: langRoot.createChildDirectory(this, language)
                val langFile = langDir.findChild(".parameters.php")
                    ?: langDir.createChildData(this, ".parameters.php")
                val langText = runCatching { VfsUtilCore.loadText(langFile) }.getOrDefault("<?php\n")
                val langKey = ComponentParametersWriter.langKey(key)
                if (!langText.contains(langKey)) {
                    val line = "\$MESS['$langKey'] = '${escape(parameterName)}';\n"
                    val content = if (langText.contains("?>")) {
                        langText.substringBeforeLast("?>").trimEnd() + "\n" + line
                    } else {
                        langText.trimEnd() + "\n" + line
                    }
                    write(langFile, content)
                }
            }
        }
    }

    private fun addParameter(text: String, key: String, parameterName: String): String {
        if (text.contains("'$key'") || text.contains("\"$key\"")) return text
        val marker = PARAMETERS.find(text) ?: return text
        val open = marker.range.last
        val close = matchingBracket(text, open) ?: return text
        val entry = buildString {
            append("        '$key' => [\n")
            append("            'PARENT' => 'BASE',\n")
            append("            'NAME' => Loc::getMessage('${ComponentParametersWriter.langKey(key)}'),\n")
            append("            'TYPE' => 'STRING',\n")
            append("        ],\n")
        }
        return text.substring(0, close) + entry + text.substring(close)
    }

    private fun write(file: VirtualFile, text: String) {
        val document = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(file)
        if (document != null) {
            document.setText(text)
            com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().saveDocument(document)
        } else {
            VfsUtil.saveText(file, text)
        }
    }

    private fun escape(value: String): String = value.replace("\\", "\\\\").replace("'", "\\'")

    private companion object {
        val PARAMETERS = Regex("""['"]PARAMETERS['"]\s*=>\s*\[""")
    }
}

/**
 * Создаёт `template.php` в существующем каталоге шаблона.
 */
class CreateComponentTemplateFileFix(
    private val templateDirectory: VirtualFile,
) : LocalQuickFix {
    override fun getFamilyName(): String = message("inspection.fix.create.template.family")

    override fun getName(): String = message("inspection.fix.create.template.name", templateDirectory.name)

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        WriteCommandAction.runWriteCommandAction(project) {
            if (templateDirectory.findChild("template.php") != null) return@runWriteCommandAction
            VfsUtil.saveText(
                templateDirectory.createChildData(this, "template.php"),
                "<?php\n\nif (!defined('B_PROLOG_INCLUDED') || B_PROLOG_INCLUDED !== true) {\n    die();\n}\n",
            )
        }
    }
}

/**
 * Создаёт каталог шаблона с `template.php`, `style.css` и `script.js`.
 */
class CreateComponentTemplateDirectoryFix(
    private val componentDirectory: VirtualFile,
    private val templateName: String,
) : LocalQuickFix {
    override fun getFamilyName(): String = message("inspection.fix.create.template.dir.family")

    override fun getName(): String = message("inspection.fix.create.template.dir.name", templateName)

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        WriteCommandAction.runWriteCommandAction(project) {
            val templates = componentDirectory.findChild("templates")
                ?: componentDirectory.createChildDirectory(this, "templates")
            if (templates.findChild(templateName) != null) return@runWriteCommandAction
            val target = templates.createChildDirectory(this, templateName)
            VfsUtil.saveText(
                target.createChildData(this, "template.php"),
                "<?php\n\nif (!defined('B_PROLOG_INCLUDED') || B_PROLOG_INCLUDED !== true) {\n    die();\n}\n",
            )
            VfsUtil.saveText(target.createChildData(this, "style.css"), "")
            VfsUtil.saveText(target.createChildData(this, "script.js"), "")
        }
    }
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

private fun matchingBracket(text: String, open: Int): Int? {
    var depth = 0
    var quote: Char? = null
    var escape = false
    for (index in open until text.length) {
        val char = text[index]
        if (quote != null) {
            if (escape) {
                escape = false
            } else if (char == '\\') {
                escape = true
            } else if (char == quote) {
                quote = null
            }
            continue
        }
        if (char == '\'' || char == '"') {
            quote = char
            continue
        }
        when (char) {
            '[' -> depth++
            ']' -> {
                depth--
                if (depth == 0) return index
            }
        }
    }
    return null
}

private fun replaceElement(project: Project, element: PsiElement, replacement: String) {
    WriteCommandAction.runWriteCommandAction(project) {
        val document = PsiDocumentManager.getInstance(project)
            .getDocument(element.containingFile) ?: return@runWriteCommandAction
        document.replaceString(element.textRange.startOffset, element.textRange.endOffset, replacement)
        PsiDocumentManager.getInstance(project).commitDocument(document)
    }
}
