package com.github.saundefined.bitrix_idea.orm

import org.junit.Assert.assertTrue
import org.junit.Test

class OrmFullStackTest {
    private val model = OrmEntityModel(
        namespace = "Vendor\\Module\\Model",
        className = "BookTable",
        tableName = "vendor_module_book",
        fields = listOf(
            OrmField("ID", OrmFieldType.INTEGER, primary = true, autocomplete = true),
            OrmField("TITLE", OrmFieldType.STRING, required = true, length = 255),
            OrmField("ACTIVE", OrmFieldType.BOOLEAN, defaultValue = "true"),
        ),
    )

    @Test
    fun generatesFullStackMethods() {
        val generated = OrmEntityGenerator.generate(model, fullStack = true)

        assertTrue(generated.fullStackMethods.contains("return Book::class;"))
        assertTrue(generated.fullStackMethods.contains("return BookCollection::class;"))
    }

    @Test
    fun generatesObjectAndCollection() {
        val objectClass = OrmEntityGenerator.generateObjectClass(model)
        val collectionClass = OrmEntityGenerator.generateCollectionClass(model)

        assertTrue(objectClass.contains("class Book extends EO_Book"))
        assertTrue(collectionClass.contains("class BookCollection extends EO_Book_Collection"))
    }

    @Test
    fun generatesSql() {
        val create = OrmSqlGenerator.createTable(model)
        val drop = OrmSqlGenerator.dropTable(model)

        assertTrue(create.contains("CREATE TABLE IF NOT EXISTS `vendor_module_book`"))
        assertTrue(create.contains("`ID` int NOT NULL AUTO_INCREMENT"))
        assertTrue(create.contains("`TITLE` varchar(255) NOT NULL"))
        assertTrue(create.contains("`ACTIVE` char(1) NULL DEFAULT 'Y'"))
        assertTrue(create.contains("PRIMARY KEY (`ID`)"))
        assertTrue(drop.contains("DROP TABLE IF EXISTS `vendor_module_book`;"))
    }
}
