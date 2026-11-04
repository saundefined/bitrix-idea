package com.github.saundefined.bitrix_idea.orm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OrmEntityValidatorTest {
    private fun validModel(): OrmEntityModel {
        return OrmEntityModel(
            namespace = "Vendor\\Module\\Orm",
            className = "BookTable",
            tableName = "vendor_module_book",
            fields = listOf(
                OrmField("ID", OrmFieldType.INTEGER, primary = true, autocomplete = true),
                OrmField("TITLE", OrmFieldType.STRING, required = true, length = 255),
            ),
        )
    }

    @Test
    fun acceptsValidModel() {
        assertNull(OrmEntityValidator.modelError(validModel()))
    }

    @Test
    fun rejectsInvalidNamespace() {
        assertEquals(
            OrmEntityValidator.ERROR_NAMESPACE,
            OrmEntityValidator.namespaceError("Vendor\\\\Module"),
        )
    }

    @Test
    fun rejectsLowercaseClassName() {
        assertEquals(
            OrmEntityValidator.ERROR_CLASS,
            OrmEntityValidator.classNameError("book"),
        )
    }

    @Test
    fun rejectsInvalidTableName() {
        assertEquals(
            OrmEntityValidator.ERROR_TABLE,
            OrmEntityValidator.tableNameError("Book-Table"),
        )
    }

    @Test
    fun rejectsEmptyFields() {
        assertEquals(
            OrmEntityValidator.ERROR_FIELDS_EMPTY,
            OrmEntityValidator.modelError(validModel().copy(fields = emptyList())),
        )
    }

    @Test
    fun rejectsDuplicateFieldNames() {
        assertEquals(
            OrmEntityValidator.ERROR_FIELD_DUPLICATE,
            OrmEntityValidator.modelError(
                validModel().copy(
                    fields = listOf(
                        OrmField("ID", OrmFieldType.INTEGER, primary = true, autocomplete = true),
                        OrmField("ID", OrmFieldType.STRING),
                    ),
                ),
            ),
        )
    }

    @Test
    fun rejectsMissingPrimaryField() {
        assertEquals(
            OrmEntityValidator.ERROR_PRIMARY_MISSING,
            OrmEntityValidator.modelError(
                validModel().copy(fields = listOf(OrmField("TITLE", OrmFieldType.STRING))),
            ),
        )
    }

    @Test
    fun rejectsAutocompleteForNonInteger() {
        assertEquals(
            OrmEntityValidator.ERROR_FIELD_AUTOCOMPLETE,
            OrmEntityValidator.fieldError(
                OrmField("ID", OrmFieldType.STRING, primary = true, autocomplete = true),
            ),
        )
    }

    @Test
    fun rejectsRequiredAndNullableTogether() {
        assertEquals(
            OrmEntityValidator.ERROR_FIELD_REQUIRED_NULLABLE,
            OrmEntityValidator.fieldError(
                OrmField("TITLE", OrmFieldType.STRING, required = true, nullable = true),
            ),
        )
    }

    @Test
    fun rejectsEnumWithoutValues() {
        assertEquals(
            OrmEntityValidator.ERROR_FIELD_VALUES,
            OrmEntityValidator.fieldError(OrmField("STATUS", OrmFieldType.ENUM)),
        )
    }

    @Test
    fun rejectsInvalidNumericDefault() {
        assertEquals(
            OrmEntityValidator.ERROR_FIELD_DEFAULT,
            OrmEntityValidator.fieldError(
                OrmField("COUNT", OrmFieldType.INTEGER, defaultValue = "many"),
            ),
        )
    }

    @Test
    fun acceptsValidReferenceRelation() {
        val error = OrmEntityValidator.relationError(
            relation = OrmRelation(
                name = "AUTHOR",
                type = OrmRelationType.REFERENCE,
                targetEntity = "Bitrix\\Main\\UserTable",
                localField = "AUTHOR_ID",
                remoteField = "ID",
            ),
            existingNames = setOf("ID", "AUTHOR_ID"),
            scalarFieldNames = setOf("ID", "AUTHOR_ID"),
        )

        assertNull(error)
    }

    @Test
    fun rejectsRelationNameThatDuplicatesScalarField() {
        assertEquals(
            OrmEntityValidator.ERROR_RELATION_DUPLICATE,
            OrmEntityValidator.relationError(
                relation = OrmRelation(
                    name = "TITLE",
                    type = OrmRelationType.REFERENCE,
                    targetEntity = "BookTable",
                    localField = "TITLE",
                ),
                existingNames = setOf("TITLE"),
                scalarFieldNames = setOf("TITLE"),
            ),
        )
    }

    @Test
    fun rejectsReferenceWithMissingLocalField() {
        assertEquals(
            OrmEntityValidator.ERROR_RELATION_LOCAL_FIELD_MISSING,
            OrmEntityValidator.relationError(
                relation = OrmRelation(
                    name = "AUTHOR",
                    type = OrmRelationType.REFERENCE,
                    targetEntity = "UserTable",
                    localField = "AUTHOR_ID",
                ),
                scalarFieldNames = setOf("ID"),
            ),
        )
    }

    @Test
    fun rejectsInvalidRelationTarget() {
        assertEquals(
            OrmEntityValidator.ERROR_RELATION_TARGET,
            OrmEntityValidator.relationError(
                relation = OrmRelation(
                    name = "AUTHOR",
                    type = OrmRelationType.REFERENCE,
                    targetEntity = "Not A Class",
                    localField = "ID",
                ),
                scalarFieldNames = setOf("ID"),
            ),
        )
    }

    @Test
    fun rejectsOneToManyWithoutReferenceField() {
        assertEquals(
            OrmEntityValidator.ERROR_RELATION_REFERENCE_FIELD,
            OrmEntityValidator.relationError(
                relation = OrmRelation(
                    name = "ITEMS",
                    type = OrmRelationType.ONE_TO_MANY,
                    targetEntity = "ItemTable",
                ),
            ),
        )
    }

    @Test
    fun rejectsExpressionWithoutSql() {
        assertEquals(
            OrmEntityValidator.ERROR_FIELD_EXPRESSION,
            OrmEntityValidator.fieldError(OrmField("AGE_DAYS", OrmFieldType.EXPRESSION)),
        )
    }

    @Test
    fun rejectsInvalidValidatorRange() {
        assertEquals(
            OrmEntityValidator.ERROR_VALIDATOR_RANGE,
            OrmEntityValidator.fieldError(
                OrmField(
                    "PAGES",
                    OrmFieldType.INTEGER,
                    validators = listOf(OrmValidator(OrmValidatorType.RANGE, min = "10", max = "1")),
                ),
            ),
        )
    }

    @Test
    fun rejectsInvalidManyToManyJoinType() {
        assertEquals(
            OrmEntityValidator.ERROR_RELATION_JOIN,
            OrmEntityValidator.relationError(
                OrmRelation(
                    name = "TAGS",
                    type = OrmRelationType.MANY_TO_MANY,
                    targetEntity = "TagTable",
                    mediatorTable = "vendor_module_book_tag",
                    joinType = "cross",
                ),
            ),
        )
    }

    @Test
    fun rejectsManyToManyWithoutMediatorTable() {
        assertEquals(
            OrmEntityValidator.ERROR_RELATION_MEDIATOR,
            OrmEntityValidator.relationError(
                relation = OrmRelation(
                    name = "TAGS",
                    type = OrmRelationType.MANY_TO_MANY,
                    targetEntity = "TagTable",
                ),
            ),
        )
    }
}
