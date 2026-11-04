package com.github.saundefined.bitrix_idea.orm

import org.junit.Assert.assertEquals
import org.junit.Test

class OrmFieldTest {
    @Test
    fun describesOptions() {
        val field = OrmField(
            name = "TITLE",
            type = OrmFieldType.STRING,
            primary = true,
            required = true,
            unique = true,
            length = 255,
        )

        assertEquals("PK, required, unique, length=255", field.optionsSummary())
    }
}
