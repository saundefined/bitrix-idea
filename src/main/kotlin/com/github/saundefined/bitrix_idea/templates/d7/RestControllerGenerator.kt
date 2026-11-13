package com.github.saundefined.bitrix_idea.templates.d7

/**
 * Генерация D7 REST-контроллера и заготовки маршрутов модуля.
 */
object RestControllerGenerator {
    data class ActionSpec(val name: String, val method: String = "GET")

    fun generate(namespace: String, className: String, actions: List<ActionSpec>): String {
        return buildString {
            appendLine("<?php")
            appendLine()
            if (namespace.isNotBlank()) {
                appendLine("namespace $namespace;")
                appendLine()
            }
            appendLine("use Bitrix\\Main\\Engine\\ActionFilter;")
            appendLine("use Bitrix\\Main\\Engine\\Controller;")
            appendLine()
            appendLine("class $className extends Controller")
            appendLine("{")
            appendLine("    public function configureActions(): array")
            appendLine("    {")
            appendLine("        return [")
            actions.forEach { action ->
                appendLine("            '${action.name}' => [")
                appendLine("                'prefilters' => [")
                appendLine("                    new ActionFilter\\HttpMethod([ActionFilter\\HttpMethod::METHOD_${action.method.uppercase()}]),")
                appendLine("                ],")
                appendLine("            ],")
            }
            appendLine("        ];")
            appendLine("    }")
            actions.forEach { action ->
                appendLine()
                appendLine("    public function ${action.name}Action(): array")
                appendLine("    {")
                appendLine("        return [];")
                appendLine("    }")
            }
            appendLine("}")
        }
    }

    fun generateRoutes(moduleId: String, namespace: String, className: String, actions: List<ActionSpec>): String {
        return buildString {
            appendLine("<?php")
            appendLine()
            appendLine("use Bitrix\\Main\\Routing\\RoutingConfigurator;")
            appendLine()
            appendLine("return function (RoutingConfigurator \$routes) {")
            actions.forEach { action ->
                val httpMethod = action.method.lowercase()
                appendLine("    \$routes->$httpMethod('/api/$moduleId/${className.lowercase()}/${action.name}/', [\\$namespace\\$className::class, '${action.name}Action']);")
            }
            appendLine("};")
        }
    }
}
