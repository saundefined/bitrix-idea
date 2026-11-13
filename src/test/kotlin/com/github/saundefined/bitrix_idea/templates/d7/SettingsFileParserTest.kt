package com.github.saundefined.bitrix_idea.templates.d7

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsFileParserTest {
    @Test
    fun parsesAndRendersSettingsFile() {
        val text = """
            <?php
            return [
                'controllers' => ['value' => ['namespaces' => ['\Vendor\Module\Controller' => 'api']]],
                'services' => ['value' => ['vendor.service' => ['className' => \Vendor\Module\Service::class]]],
                'events' => ['value' => ['main' => ['OnBeforeProlog' => [['\Vendor\Module\Handlers', 'onBeforeProlog']]]]],
                'agents' => ['value' => ['\Vendor\Module\Agent::run' => 3600]],
                'console' => ['value' => ['commands' => [\Vendor\Module\Command::class]]],
            ];
        """.trimIndent()

        val model = SettingsFileParser.parse(text)

        assertEquals(listOf("Vendor\\Module\\Controller"), model.controllers)
        assertEquals("Vendor\\Module\\Service", model.services.first().className)
        assertTrue(model.events.any { it.event == "OnBeforeProlog" && it.method == "onBeforeProlog" })
        assertEquals(3600, model.agents.first().interval)
        assertEquals(listOf("Vendor\\Module\\Command"), model.console)

        val rendered = SettingsFileParser.render(model)
        assertTrue(rendered.contains("'controllers'"))
        assertTrue(rendered.contains("'events'"))
        assertTrue(rendered.contains("Vendor\\\\Module\\\\Agent::run"))
    }

    @Test
    fun updatePreservesCommentsOutsideSections() {
        val original = """
            <?php
            // important comment
            return [
                'controllers' => ['value' => ['namespaces' => ['Old\\Controller' => 'api']]],
            ];
        """.trimIndent()
        val updated = SettingsFileParser.update(
            original,
            SettingsFileModel(
                controllers = listOf("Vendor\\Module\\Controller"),
                services = listOf(SettingsServiceEntry("vendor.service", "Vendor\\Module\\Service")),
            ),
        )

        assertTrue(updated.contains("// important comment"))
        assertTrue(updated.contains("Vendor\\\\Module\\\\Controller"))
        assertTrue(updated.contains("'services'"))
        assertTrue(!updated.contains("Old\\Controller"))
    }
}
