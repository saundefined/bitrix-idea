package com.github.saundefined.bitrix_idea.templates.d7

data class SettingsEventEntry(
    val module: String,
    val event: String,
    val className: String,
    val method: String,
)

data class SettingsServiceEntry(
    val key: String,
    val className: String,
)

data class SettingsAgentEntry(
    val callable: String,
    val interval: Int,
)

data class SettingsFileModel(
    val controllers: List<String> = emptyList(),
    val services: List<SettingsServiceEntry> = emptyList(),
    val events: List<SettingsEventEntry> = emptyList(),
    val agents: List<SettingsAgentEntry> = emptyList(),
    val console: List<String> = emptyList(),
)

/**
 * Разбор и нормализованная генерация `.settings.php`.
 *
 * Поддерживаются типовые секции controllers/services/events/agents/console.
 * Комментарии и нестандартное форматирование при сохранении не сохраняются,
 * поэтому редактор показывает предпросмотр.
 */
object SettingsFileParser {
    private val CONTROLLERS = Regex("""['"]([\\A-Za-z_][\\A-Za-z0-9_]*)['"]\s*=>\s*['"]api['"]""")
    private val SERVICES = Regex("""['"]([^'"]+)['"]\s*=>\s*\[\s*['"]className['"]\s*=>\s*([\\A-Za-z_][\\A-Za-z0-9_]*)::class""", RegexOption.DOT_MATCHES_ALL)
    private val LIST_EVENTS = Regex("""\[\s*['"](On[A-Za-z0-9_]+)['"]\s*,\s*['"]([\\A-Za-z_][\\A-Za-z0-9_]*)['"]\s*,\s*['"]([A-Za-z_][A-Za-z0-9_]*)['"]\s*]""")
    private val AGENTS = Regex("""['"]([\\A-Za-z_][\\A-Za-z0-9_]*::[A-Za-z_][A-Za-z0-9_]*)['"]\s*=>\s*(\d+)""")
    private val EVENT_CALLABLE = Regex("""['"]([\\A-Za-z_][\\A-Za-z0-9_]*)['"]\s*,\s*['"]([A-Za-z_][A-Za-z0-9_]*)['"]""")

    fun parse(text: String): SettingsFileModel {
        val controllers = CONTROLLERS.findAll(text)
            .map { it.groupValues[1].trimStart('\\') }
            .distinct()
            .toList()

        val services = SERVICES.findAll(text)
            .map { SettingsServiceEntry(it.groupValues[1], it.groupValues[2].trimStart('\\')) }
            .distinctBy { it.key }
            .toList()

        val listEvents = LIST_EVENTS.findAll(text).map {
            SettingsEventEntry("", it.groupValues[1], it.groupValues[2].trimStart('\\'), it.groupValues[3])
        }.toList()

        val keyedEvents = parseKeyedEvents(text)
        val events = (listEvents + keyedEvents).distinctBy { "${it.event}|${it.className}|${it.method}" }

        val agents = AGENTS.findAll(text)
            .map { SettingsAgentEntry(it.groupValues[1].trimStart('\\'), it.groupValues[2].toIntOrNull() ?: 86400) }
            .distinctBy { it.callable }
            .toList()

        val console = parseConsole(text)

        return SettingsFileModel(controllers, services, events, agents, console)
    }

    fun render(model: SettingsFileModel): String {
        return buildString {
            appendLine("<?php")
            appendLine()
            appendLine("return [")
            if (model.controllers.isNotEmpty()) {
                appendLine("    'controllers' => [")
                appendLine("        'value' => [")
                appendLine("            'namespaces' => [")
                model.controllers.forEach { namespace ->
                    appendLine("                '${escape(namespace)}' => 'api',")
                }
                appendLine("            ],")
                appendLine("        ],")
                appendLine("    ],")
            }
            if (model.services.isNotEmpty()) {
                appendLine("    'services' => [")
                appendLine("        'value' => [")
                model.services.forEach { service ->
                    appendLine("            '${escape(service.key)}' => [")
                    appendLine("                'className' => \\${service.className}::class,")
                    appendLine("            ],")
                }
                appendLine("        ],")
                appendLine("    ],")
            }
            if (model.events.isNotEmpty()) {
                appendLine("    'events' => [")
                appendLine("        'value' => [")
                model.events.groupBy { it.module.ifBlank { "main" } }.forEach { (module, entries) ->
                    appendLine("            '${escape(module)}' => [")
                    entries.forEach { entry ->
                        appendLine("                '${escape(entry.event)}' => [")
                        appendLine("                    ['${escape(entry.className)}', '${escape(entry.method)}'],")
                        appendLine("                ],")
                    }
                    appendLine("            ],")
                }
                appendLine("        ],")
                appendLine("    ],")
            }
            if (model.agents.isNotEmpty()) {
                appendLine("    'agents' => [")
                appendLine("        'value' => [")
                model.agents.forEach { agent ->
                    appendLine("            '${escape(agent.callable)}' => ${agent.interval},")
                }
                appendLine("        ],")
                appendLine("    ],")
            }
            if (model.console.isNotEmpty()) {
                appendLine("    'console' => [")
                appendLine("        'value' => [")
                appendLine("            'commands' => [")
                model.console.forEach { command ->
                    appendLine("                \\${command}::class,")
                }
                appendLine("            ],")
                appendLine("        ],")
                appendLine("    ],")
            }
            appendLine("];")
        }
    }

    private fun parseKeyedEvents(text: String): List<SettingsEventEntry> {
        val section = section(text, "events") ?: return emptyList()
        val moduleRegex = Regex("""['"]([a-z0-9_]+)['"]\s*=>\s*\[""")
        val modules = moduleRegex.findAll(section)
        val result = mutableListOf<SettingsEventEntry>()
        modules.forEach { moduleMatch ->
            val module = moduleMatch.groupValues[1]
            val open = moduleMatch.range.last
            val close = matchingBracket(section, open) ?: return@forEach
            val moduleBody = section.substring(open + 1, close)
            val eventRegex = Regex("""['"](On[A-Za-z0-9_]+)['"]\s*=>\s*\[""")
            eventRegex.findAll(moduleBody).forEach { eventMatch ->
                val event = eventMatch.groupValues[1]
                val eventOpen = eventMatch.range.last
                val eventClose = matchingBracket(moduleBody, eventOpen) ?: return@forEach
                val eventBody = moduleBody.substring(eventOpen + 1, eventClose)
                EVENT_CALLABLE.findAll(eventBody).forEach { callable ->
                    result += SettingsEventEntry(
                        module = module,
                        event = event,
                        className = callable.groupValues[1],
                        method = callable.groupValues[2],
                    )
                }
            }
        }
        return result
    }

    private fun parseConsole(text: String): List<String> {
        val section = section(text, "console") ?: return emptyList()
        return Regex("""(\\?[A-Za-z_][A-Za-z0-9_\\]*)::class""")
            .findAll(section)
            .map { it.groupValues[1].trimStart('\\') }
            .distinct()
            .toList()
    }

    private fun section(text: String, key: String): String? {
        val match = Regex("""['"]$key['"]\s*=>\s*\[""").find(text) ?: return null
        val open = match.range.last
        val close = matchingBracket(text, open) ?: return null
        return text.substring(open, close + 1)
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

    private fun escape(value: String): String = value.replace("\\", "\\\\").replace("'", "\\'")
}
