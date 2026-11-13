package com.github.saundefined.bitrix_idea.templates.d7

/**
 * Добавляет AJAX-действие и фильтры в существующий D7-контроллер.
 */
object AjaxActionGenerator {
    fun addAction(
        text: String,
        actionName: String,
        parameters: List<String>,
        prefilters: List<String>,
    ): String {
        val name = actionName.trim().removeSuffix("Action")
        if (name.isBlank()) return text
        val methodName = "${name}Action"

        var result = text
        if (!result.contains("function $methodName")) {
            val signature = if (parameters.isEmpty()) {
                "public function $methodName(): array"
            } else {
                "public function $methodName(" + parameters.joinToString(", ") { "\$$it = null" } + "): array"
            }
            val method = buildString {
                appendLine()
                appendLine("    $signature")
                appendLine("    {")
                appendLine("        return [];")
                appendLine("    }")
            }
            val classEnd = result.lastIndexOf('}')
            if (classEnd >= 0) {
                result = result.substring(0, classEnd) + method + result.substring(classEnd)
            }
        }

        return addToConfigureActions(result, name, prefilters)
    }

    private fun addToConfigureActions(text: String, name: String, prefilters: List<String>): String {
        if (text.contains("'$name' =>")) return text
        val configure = text.indexOf("function configureActions")
        if (configure < 0) return text
        val returnIndex = text.indexOf("return", configure)
        if (returnIndex < 0) return text
        val open = text.indexOf('[', returnIndex)
        if (open < 0) return text
        val close = matchingBracket(text, open) ?: return text

        val arguments = prefilters.joinToString(", ") { "\\${it.trim().removePrefix("\\")}::class" }
        val entry = if (arguments.isEmpty()) {
            "            '$name' => [],\n"
        } else {
            "            '$name' => [\n                'prefilters' => [$arguments],\n            ],\n"
        }
        return text.substring(0, close) + entry + text.substring(close)
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
}
