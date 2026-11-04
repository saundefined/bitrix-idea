package com.github.saundefined.bitrix_idea.orm

import org.junit.Assert.assertTrue
import org.junit.Test

class OrmAdvancedGeneratorTest {
    private val model = OrmEntityModel(
        namespace = "Vendor\\Module\\Model",
        className = "BookTable",
        tableName = "vendor_module_book",
        fields = listOf(
            OrmField("ID", OrmFieldType.INTEGER, primary = true, autocomplete = true),
            OrmField(
                "ISBN",
                OrmFieldType.STRING,
                required = true,
                length = 13,
                validators = listOf(
                    OrmValidator(OrmValidatorType.REGEXP, pattern = "/^\\d{13}$/"),
                    OrmValidator(OrmValidatorType.UNIQUE),
                ),
            ),
            OrmField(
                "AGE_DAYS",
                OrmFieldType.EXPRESSION,
                expression = "DATEDIFF(NOW(), %s)",
                expressionFields = listOf("PUBLISH_DATE"),
            ),
        ),
        relations = listOf(
            OrmRelation(
                name = "AUTHORS",
                type = OrmRelationType.MANY_TO_MANY,
                targetEntity = "Vendor\\Module\\Model\\AuthorTable",
                mediatorTable = "vendor_module_book_author",
            ),
        ),
    )

    @Test
    fun generatesValidatorsAndExpression() {
        val generated = OrmEntityGenerator.generate(model)

        assertTrue(generated.fields.contains("new LengthValidator(null, 13)"))
        assertTrue(generated.fields.contains("new RegExpValidator('/^\\\\d{13}$/')"))
        assertTrue(generated.fields.contains("new UniqueValidator()"))
        assertTrue(generated.fields.contains("new ExpressionField('AGE_DAYS', 'DATEDIFF(NOW(), %s)', ['PUBLISH_DATE'])"))
    }

    @Test
    fun generatesManyToManyTableName() {
        val generated = OrmEntityGenerator.generate(model)
        assertTrue(generated.fields.contains("->configureTableName('vendor_module_book_author')"))
    }

    @Test
    fun generatesInstallerEntriesAndAnnotations() {
        val entries = OrmInstallerGenerator.entityEntries(model)
        assertTrue(entries.any { it.contains("BookTable::getEntity()") })
        assertTrue(entries.any { it.contains("getField('AUTHORS')->getMediatorEntity()") })

        val annotations = OrmInstallerGenerator.annotationBlock(model, "lib/Model/BookTable.php")
        assertTrue(annotations.contains("class EO_Book extends"))
        assertTrue(annotations.contains("class EO_Book_Collection extends"))
        assertTrue(annotations.contains("@method static EO_Book_Query query()"))
        assertTrue(annotations.contains("@method int getId()"))
        assertTrue(annotations.contains("@method void addToAuthors"))
        assertTrue(!annotations.contains("\\\\"))
    }
}
