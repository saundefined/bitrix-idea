package com.github.saundefined.bitrix_idea.orm

import org.junit.Assert.assertEquals
import org.junit.Test

class OrmNameUtilsTest {
    @Test
    fun normalizesClassName() {
        assertEquals("BookTable", OrmNameUtils.normalizeClassName("Book"))
        assertEquals("BookTable", OrmNameUtils.normalizeClassName("BookTable"))
        assertEquals("", OrmNameUtils.normalizeClassName(""))
    }

    @Test
    fun convertsToSnakeCase() {
        assertEquals("book", OrmNameUtils.toSnakeCase("Book"))
        assertEquals("book_item", OrmNameUtils.toSnakeCase("BookItem"))
        assertEquals("my_url", OrmNameUtils.toSnakeCase("MyURL"))
    }
}
