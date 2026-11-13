package com.github.saundefined.bitrix_idea.templates.events

import org.junit.Assert.assertTrue
import org.junit.Test

class HandlerGeneratorTest {
    @Test
    fun generatesEventHandler() {
        val code = HandlerGenerator.eventHandler("Vendor\\Module", "Handlers", "onBeforeProlog", "OnBeforeProlog")

        assertTrue(code.contains("class Handlers"))
        assertTrue(code.contains("public static function onBeforeProlog(): void"))
        assertTrue(code.contains("OnBeforeProlog"))
    }

    @Test
    fun generatesAgent() {
        val code = HandlerGenerator.agent("Vendor\\Module", "Agent", "run")

        assertTrue(code.contains("class Agent"))
        assertTrue(code.contains("public static function run(): string"))
    }
}
