package com.github.saundefined.bitrix_idea.templates.d7

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AjaxActionGeneratorTest {
    @Test
    fun addsActionAndPrefilter() {
        val source = """
            <?php
            class Items extends \Bitrix\Main\Engine\Controller
            {
                public function configureActions(): array
                {
                    return [];
                }
            }
        """.trimIndent()

        val updated = AjaxActionGenerator.addAction(
            text = source,
            actionName = "save",
            parameters = listOf("id"),
            prefilters = listOf("Vendor\\Module\\Filter"),
        )

        assertTrue(updated.contains("public function saveAction(\$id = null): array"))
        assertTrue(updated.contains("'save' => ["))
        assertTrue(updated.contains("'prefilters' => [\\Vendor\\Module\\Filter::class]"))
    }

    @Test
    fun doesNotDuplicateExistingAction() {
        val source = """
            <?php
            class Items extends \Bitrix\Main\Engine\Controller
            {
                public function configureActions(): array
                {
                    return [
                        'save' => [],
                    ];
                }

                public function saveAction(): array
                {
                    return [];
                }
            }
        """.trimIndent()

        val updated = AjaxActionGenerator.addAction(source, "save", emptyList(), emptyList())
        assertFalse(updated.contains("public function saveAction(): array\n    {\n        return [];\n    }\n\n    public function saveAction"))
    }
}
