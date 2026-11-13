package com.github.saundefined.bitrix_idea.php

import com.github.saundefined.bitrix_idea.php.inspections.MissingLangMessageInspection
import com.github.saundefined.bitrix_idea.templates.d7.D7SettingsRegistrar
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class BitrixPsiTest : BasePlatformTestCase() {
    fun testFindsLanguageDefinitions() {
        myFixture.addFileToProject(
            "local/components/vendor/test/lang/ru/test.php",
            "<?php\n\$MESS['HELLO'] = 'Привет';\n",
        )
        val file = myFixture.configureByText(PhpFileType.INSTANCE, "<?php Loc::getMessage('HELLO');")

        val definitions = BitrixLangSearch.findDefinitions(project, "HELLO")
        val usages = BitrixLangSearch.findUsages(project, "HELLO")

        assertTrue(definitions.isNotEmpty())
        assertTrue(usages.isNotEmpty())
        assertEquals("HELLO", usages.first().contents)
        assertEquals(file.virtualFile, usages.first().containingFile.virtualFile)
    }

    fun testFoldingSubstitutesMessageParameters() {
        myFixture.addFileToProject(
            "local/components/vendor/test/lang/ru/test.php",
            "<?php\n\$MESS['GREETING'] = 'Привет, #NAME#!';\n",
        )
        val file = myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php Loc::getMessage('GREETING', ['#NAME#' => 'Иван']);",
        )
        val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: error("No document")

        val regions = BitrixLangFoldingBuilder().buildFoldRegions(file, document, false)

        assertTrue(regions.isNotEmpty())
        assertEquals("\"Привет, Иван!\"", regions.first().placeholderText)
    }

    fun testFoldingShowsLanguageText() {
        myFixture.addFileToProject(
            "local/components/vendor/test/lang/ru/test.php",
            "<?php\n\$MESS['HELLO'] = 'Привет';\n",
        )
        val file = myFixture.configureByText(PhpFileType.INSTANCE, "<?php Loc::getMessage('HELLO');")
        val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: error("No document")

        val regions = BitrixLangFoldingBuilder().buildFoldRegions(file, document, false)

        assertTrue(regions.isNotEmpty())
        assertEquals("\"Привет\"", regions.first().placeholderText)
    }

    fun testMissingLangMessageInspection() {
        myFixture.enableInspections(MissingLangMessageInspection())
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php Loc::getMessage('MISSING_KEY');")

        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.any { it.description?.contains("MISSING_KEY") == true })
    }

    fun testSettingsEventRegistration() {
        val settings = myFixture.addFileToProject(
            "local/modules/vendor.module/.settings.php",
            "<?php\n\nreturn [];\n",
        )
        val moduleRoot = settings.virtualFile.parent

        ApplicationManager.getApplication().runWriteAction {
            D7SettingsRegistrar.registerEvent(
                moduleRoot = moduleRoot,
                moduleId = "main",
                eventName = "OnBeforeProlog",
                handlerFqn = "\\Vendor\\Module\\Handlers",
                methodName = "handle",
            )
            D7SettingsRegistrar.registerAgent(moduleRoot, "\\Vendor\\Module\\Agent::run")
        }

        val text = VfsUtilCore.loadText(settings.virtualFile)
        assertTrue(text.contains("'events'"))
        assertTrue(text.contains("'OnBeforeProlog'"))
        assertTrue(text.contains("Vendor\\\\Module\\\\Handlers"))
        assertTrue(text.contains("'agents'"))
        assertTrue(text.contains("Vendor\\\\Module\\\\Agent::run"))
    }

    fun testComponentParameterResolvesFromTemplate() {
        myFixture.addFileToProject(
            "local/components/vendor/test/.description.php",
            "<?php\n\$arComponentDescription = ['NAME' => 'Test'];\n",
        )
        myFixture.addFileToProject(
            "local/components/vendor/test/.parameters.php",
            """
            <?php
            ${'$'}arComponentParameters = [
                'PARAMETERS' => [
                    'IBLOCK_ID' => [
                        'NAME' => 'Инфоблок',
                        'TYPE' => 'NUMBER',
                    ],
                ],
            ];
            """.trimIndent(),
        )
        val template = myFixture.addFileToProject(
            "local/components/vendor/test/templates/.default/template.php",
            "<?php echo \$arParams['IBLOCK_ID'];",
        )

        val literal = PsiTreeUtil.findChildrenOfType(template, StringLiteralExpression::class.java)
            .firstOrNull { it.contents == "IBLOCK_ID" }
        assertNotNull(literal)
        val parameter = BitrixComponentMeta.parameterForLiteral(literal!!)

        assertEquals("vendor:test", parameter?.first)
        assertEquals("IBLOCK_ID", parameter?.second?.key)
    }
}
