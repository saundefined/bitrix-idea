package com.github.saundefined.bitrix_idea.templates.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComponentClassGeneratorTest {
    @Test
    fun buildsClassName() {
        assertEquals("NewsListComponent", ComponentClassGenerator.classNameFor("news.list"))
        assertEquals("ItemsComponent", ComponentClassGenerator.classNameFor("items"))
    }

    @Test
    fun generatesClassWithCacheAndAjax() {
        val code = ComponentClassGenerator.generate("ItemsComponent", cache = true, ajax = true)

        assertTrue(code.contains("class ItemsComponent extends \\CBitrixComponent"))
        assertTrue(code.contains("executeComponent"))
        assertTrue(code.contains("startResultCache"))
        assertTrue(code.contains("configureActions"))
    }
}
