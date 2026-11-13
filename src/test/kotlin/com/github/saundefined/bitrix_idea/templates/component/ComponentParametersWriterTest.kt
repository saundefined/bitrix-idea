package com.github.saundefined.bitrix_idea.templates.component

import org.junit.Assert.assertTrue
import org.junit.Test

class ComponentParametersWriterTest {
    @Test
    fun generatesParametersAndLang() {
        val parameters = listOf(
            ComponentParameterSpec("IBLOCK_ID", "Инфоблок", ComponentParameterType.NUMBER, "1"),
            ComponentParameterSpec("SHOW_TITLE", "Показывать заголовок", ComponentParameterType.CHECKBOX, "Y"),
        )

        val code = ComponentParametersWriter.generate(parameters)
        val lang = ComponentParametersWriter.generateLang(parameters)

        assertTrue(code.contains("'IBLOCK_ID' => ["))
        assertTrue(code.contains("'NAME' => Loc::getMessage('PARAM_IBLOCK_ID')"))
        assertTrue(code.contains("'TYPE' => 'NUMBER'"))
        assertTrue(lang.contains("\$MESS['PARAM_IBLOCK_ID'] = 'Инфоблок';"))
        assertTrue(lang.contains("\$MESS['PARAM_SHOW_TITLE'] = 'Показывать заголовок';"))
    }
}
