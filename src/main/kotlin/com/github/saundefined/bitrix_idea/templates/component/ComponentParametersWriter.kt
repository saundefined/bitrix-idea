package com.github.saundefined.bitrix_idea.templates.component

import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.util.Locale

enum class ComponentParameterType(val displayName: String) {
    STRING("STRING"),
    LIST("LIST"),
    CHECKBOX("CHECKBOX"),
    FILE("FILE"),
    COLOR("COLOR"),
    NUMBER("NUMBER"),
}

data class ComponentParameterSpec(
    val key: String,
    val name: String,
    val type: ComponentParameterType = ComponentParameterType.STRING,
    val defaultValue: String = "",
    val parent: String = "BASE",
    val required: Boolean = false,
)

/**
 * Генерирует `.parameters.php` и lang-файлы для компонента.
 */
object ComponentParametersWriter {
    fun write(componentDirectory: VirtualFile, parameters: List<ComponentParameterSpec>, languages: List<String>) {
        val text = generate(parameters)
        val file = componentDirectory.findChild(".parameters.php")
            ?: componentDirectory.createChildData(this, ".parameters.php")
        VfsUtil.saveText(file, text)

        val langRoot = componentDirectory.findChild("lang")
            ?: componentDirectory.createChildDirectory(this, "lang")
        languages.ifEmpty { listOf("ru") }.forEach { language ->
            val langDir = langRoot.findChild(language) ?: langRoot.createChildDirectory(this, language)
            val langFile = langDir.findChild(".parameters.php")
                ?: langDir.createChildData(this, ".parameters.php")
            VfsUtil.saveText(langFile, generateLang(parameters))
        }
    }

    fun generate(parameters: List<ComponentParameterSpec>): String {
        val body = parameters.joinToString(",\n") { parameter ->
            buildString {
                append("        '").append(escape(parameter.key)).append("' => [\n")
                append("            'PARENT' => '").append(escape(parameter.parent.ifBlank { "BASE" })).append("',\n")
                append("            'NAME' => Loc::getMessage('").append(langKey(parameter.key)).append("'),\n")
                append("            'TYPE' => '").append(parameter.type.displayName).append("',\n")
                if (parameter.defaultValue.isNotBlank()) {
                    append("            'DEFAULT' => '").append(escape(parameter.defaultValue)).append("',\n")
                }
                append("        ]")
            }
        }
        return buildString {
            appendLine("<?php")
            appendLine()
            appendLine("if (!defined('B_PROLOG_INCLUDED') || B_PROLOG_INCLUDED !== true) {")
            appendLine("    die();")
            appendLine("}")
            appendLine()
            appendLine("\$arComponentParameters = [")
            appendLine("    'PARAMETERS' => [")
            if (body.isNotBlank()) appendLine(body)
            appendLine("    ],")
            appendLine("];")
        }
    }

    fun generateLang(parameters: List<ComponentParameterSpec>): String {
        return buildString {
            appendLine("<?php")
            appendLine()
            parameters.forEach { parameter ->
                appendLine("\$MESS['${langKey(parameter.key)}'] = '${escape(parameter.name)}';")
            }
        }
    }

    fun langKey(key: String): String = "PARAM_${key.uppercase(Locale.ROOT).replace(Regex("[^A-Z0-9_]+"), "_")}"

    private fun escape(value: String): String = value.replace("\\", "\\\\").replace("'", "\\'")
}
