package com.github.saundefined.bitrix_idea.templates.d7

/**
 * Генерация сервиса и его интерфейса.
 */
object ServiceGenerator {
    fun generateInterface(namespace: String, interfaceName: String): String {
        return buildString {
            appendLine("<?php")
            appendLine()
            if (namespace.isNotBlank()) {
                appendLine("namespace $namespace;")
                appendLine()
            }
            appendLine("interface $interfaceName")
            appendLine("{")
            appendLine("}")
        }
    }

    fun generateImplementation(namespace: String, interfaceName: String, className: String): String {
        return buildString {
            appendLine("<?php")
            appendLine()
            if (namespace.isNotBlank()) {
                appendLine("namespace $namespace;")
                appendLine()
            }
            appendLine("class $className implements $interfaceName")
            appendLine("{")
            appendLine("}")
        }
    }
}
