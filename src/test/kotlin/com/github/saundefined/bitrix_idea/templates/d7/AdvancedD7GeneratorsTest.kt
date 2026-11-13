package com.github.saundefined.bitrix_idea.templates.d7

import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedD7GeneratorsTest {
    @Test
    fun generatesRestControllerAndRoutes() {
        val actions = listOf(
            RestControllerGenerator.ActionSpec("list", "GET"),
            RestControllerGenerator.ActionSpec("add", "POST"),
        )
        val controller = RestControllerGenerator.generate("Vendor\\Module\\Controller", "Items", actions)
        val routes = RestControllerGenerator.generateRoutes("vendor.module", "Vendor\\Module\\Controller", "Items", actions)

        assertTrue(controller.contains("class Items extends Controller"))
        assertTrue(controller.contains("METHOD_GET"))
        assertTrue(controller.contains("public function addAction(): array"))
        assertTrue(routes.contains("\$routes->get('/api/vendor.module/items/list/'"))
        assertTrue(routes.contains("\\Vendor\\Module\\Controller\\Items::class"))
    }

    @Test
    fun generatesConsoleCommand() {
        val code = ConsoleCommandGenerator.generate(
            namespace = "Vendor\\Module\\Console",
            className = "Command",
            commandName = "vendor:module:run",
            description = "Запуск",
            arguments = listOf("limit"),
        )

        assertTrue(code.contains("class Command extends \\Symfony\\Component\\Console\\Command\\Command"))
        assertTrue(code.contains("setName('vendor:module:run')"))
        assertTrue(code.contains("addArgument('limit'"))
    }
}
