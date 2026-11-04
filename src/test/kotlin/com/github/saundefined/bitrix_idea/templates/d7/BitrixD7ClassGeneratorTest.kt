package com.github.saundefined.bitrix_idea.templates.d7

import org.junit.Assert.assertTrue
import org.junit.Test

class BitrixD7ClassGeneratorTest {
    @Test
    fun generatesAjaxController() {
        val code = BitrixD7ClassGenerator.generate(
            D7ClassType.AJAX_CONTROLLER,
            "Vendor\\Module\\Controller",
            "Items",
        )

        assertTrue(code.contains("namespace Vendor\\Module\\Controller;"))
        assertTrue(code.contains("class Items extends \\Bitrix\\Main\\Engine\\Controller"))
        assertTrue(code.contains("configureActions"))
        assertTrue(code.contains("listAction"))
    }

    @Test
    fun generatesEventHandler() {
        val code = BitrixD7ClassGenerator.generate(
            D7ClassType.EVENT_HANDLER,
            "Vendor\\Module",
            "UserHandler",
        )

        assertTrue(code.contains("class UserHandler"))
        assertTrue(code.contains("public static function handle(\\Bitrix\\Main\\Event \$event): void"))
    }
}
