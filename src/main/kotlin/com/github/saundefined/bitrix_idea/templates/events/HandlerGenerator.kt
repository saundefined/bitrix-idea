package com.github.saundefined.bitrix_idea.templates.events

/**
 * Генерация классов обработчиков событий и агентов.
 */
object HandlerGenerator {
    fun eventHandler(namespace: String, className: String, methodName: String, eventName: String): String {
        return buildString {
            appendLine("<?php")
            appendLine()
            if (namespace.isNotBlank()) {
                appendLine("namespace $namespace;")
                appendLine()
            }
            appendLine("/**")
            appendLine(" * Обработчик события $eventName.")
            appendLine(" */")
            appendLine("class $className")
            appendLine("{")
            appendLine("    public static function $methodName(): void")
            appendLine("    {")
            appendLine("    }")
            appendLine("}")
        }
    }

    fun agent(namespace: String, className: String, methodName: String): String {
        return buildString {
            appendLine("<?php")
            appendLine()
            if (namespace.isNotBlank()) {
                appendLine("namespace $namespace;")
                appendLine()
            }
            appendLine("class $className")
            appendLine("{")
            appendLine("    public static function $methodName(): string")
            appendLine("    {")
            appendLine("        // Верните '\\Vendor\\Module\\Class::$methodName();', чтобы агент продолжил работу.")
            appendLine("        return '';")
            appendLine("    }")
            appendLine("}")
        }
    }
}
