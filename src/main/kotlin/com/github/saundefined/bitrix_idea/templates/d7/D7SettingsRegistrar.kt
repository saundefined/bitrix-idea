package com.github.saundefined.bitrix_idea.templates.d7

import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile

object D7SettingsRegistrar {
    fun register(
        moduleRoot: VirtualFile,
        type: D7ClassType,
        namespace: String,
        className: String,
    ) {
        val file = moduleRoot.findChild(".settings.php")
            ?: moduleRoot.createChildData(this, ".settings.php")
        val existing = runCatching { VfsUtilCore.loadText(file) }.getOrDefault("")
        val updated = when (type) {
            D7ClassType.CONTROLLER, D7ClassType.AJAX_CONTROLLER -> registerController(existing, namespace)
            D7ClassType.SERVICE -> registerService(existing, namespace, className)
            D7ClassType.CONSOLE -> registerConsole(existing, namespace, className)
            else -> existing
        }
        VfsUtil.saveText(file, updated)
    }

    private fun registerController(text: String, namespace: String): String {
        val phpNamespace = "\\\\" + namespace.replace("\\", "\\\\") + "\\\\Controller"
        if (text.contains(phpNamespace)) return text

        val section = findSection(text, "controllers")
        if (section == null) {
            return appendSection(
                text,
                """
                |    'controllers' => [
                |        'value' => [
                |            'namespaces' => [
                |                '$phpNamespace' => 'api',
                |            ],
                |        ],
                |    ],
                |""".trimMargin(),
            )
        }

        val value = findKeyArray(text, "value", section.first, section.last) ?: return text
        val namespaces = findKeyArray(text, "namespaces", value.first, value.second)
        return if (namespaces == null) {
            insertBefore(
                text,
                value.second,
                "            'namespaces' => [\n                '$phpNamespace' => 'api',\n            ],\n",
            )
        } else {
            insertBefore(text, namespaces.second, "                '$phpNamespace' => 'api',\n")
        }
    }

    private fun registerService(text: String, namespace: String, className: String): String {
        val fqn = "\\$namespace\\$className"
        if (text.contains(fqn)) return text
        val key = "$namespace.$className".lowercase().replace("\\", ".")
        val entry = "            '$key' => [\n                'className' => $fqn::class,\n            ],\n"

        val section = findSection(text, "services")
        if (section == null) {
            return appendSection(
                text,
                """
                |    'services' => [
                |        'value' => [
                |$entry
                |        ],
                |    ],
                |""".trimMargin(),
            )
        }
        val value = findKeyArray(text, "value", section.first, section.last) ?: return text
        return insertBefore(text, value.second, entry)
    }

    private fun registerConsole(text: String, namespace: String, className: String): String {
        val fqn = "\\$namespace\\$className"
        if (text.contains(fqn)) return text
        val entry = "                $fqn::class,\n"

        val section = findSection(text, "console")
        if (section == null) {
            return appendSection(
                text,
                """
                |    'console' => [
                |        'value' => [
                |            'commands' => [
                |$entry
                |            ],
                |        ],
                |    ],
                |""".trimMargin(),
            )
        }
        val value = findKeyArray(text, "value", section.first, section.last) ?: return text
        val commands = findKeyArray(text, "commands", value.first, value.second)
        return if (commands == null) {
            insertBefore(
                text,
                value.second,
                "            'commands' => [\n$entry            ],\n",
            )
        } else {
            insertBefore(text, commands.second, entry)
        }
    }

    private fun appendSection(text: String, section: String): String {
        if (text.isBlank()) return "<?php\n\nreturn [\n$section\n];\n"
        val index = text.lastIndexOf("];")
        return if (index < 0) {
            text.trimEnd() + "\n\n" + section
        } else {
            text.substring(0, index) + section + text.substring(index)
        }
    }

    private fun findSection(text: String, key: String): IntRange? {
        val array = findKeyArray(text, key, 0, text.length) ?: return null
        return array.first..array.second
    }

    /**
     * Находит массив `'key' => [ ... ]` и возвращает индексы открывающей и закрывающей скобок.
     */
    private fun findKeyArray(text: String, key: String, from: Int, to: Int): Pair<Int, Int>? {
        val regex = Regex("""['"]$key['"]\s*=>\s*\[""")
        val match = regex.find(text.substring(from, to)) ?: return null
        val open = from + match.range.last
        val close = matchingBracket(text, open) ?: return null
        return open to close
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

    private fun insertBefore(text: String, index: Int, content: String): String {
        return text.substring(0, index) + content + text.substring(index)
    }
}
