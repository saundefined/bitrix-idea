package com.github.saundefined.bitrix_idea.orm

import org.junit.Assert.assertTrue
import org.junit.Test

class OrmEntityGeneratorTest {
    @Test
    fun generatesImportsAndFields() {
        val model = OrmEntityModel(
            namespace = "Vendor\\Module\\Orm",
            className = "BookTable",
            tableName = "vendor_module_book",
            fields = listOf(
                OrmField("ID", OrmFieldType.INTEGER, primary = true, autocomplete = true),
                OrmField("TITLE", OrmFieldType.STRING, required = true, length = 255),
                OrmField("DESCRIPTION", OrmFieldType.TEXT, nullable = true),
                OrmField("ACTIVE", OrmFieldType.BOOLEAN, defaultValue = "true"),
                OrmField("STATUS", OrmFieldType.ENUM, values = listOf("N", "Y")),
            ),
        )

        val generated = OrmEntityGenerator.generate(model)

        assertTrue(generated.imports.contains("use Bitrix\\Main\\ORM\\Data\\DataManager;"))
        assertTrue(generated.imports.contains("use Bitrix\\Main\\ORM\\Fields\\IntegerField;"))
        assertTrue(generated.imports.contains("use Bitrix\\Main\\ORM\\Fields\\StringField;"))
        assertTrue(generated.imports.contains("use Bitrix\\Main\\ORM\\Fields\\TextField;"))
        assertTrue(generated.imports.contains("use Bitrix\\Main\\ORM\\Fields\\BooleanField;"))
        assertTrue(generated.imports.contains("use Bitrix\\Main\\ORM\\Fields\\EnumField;"))
        assertTrue(generated.imports.contains("use Bitrix\\Main\\ORM\\Fields\\Validators\\LengthValidator;"))

        assertTrue(generated.fields.contains("(new IntegerField('ID'))\n                ->configurePrimary()\n                ->configureAutocomplete(),"))
        assertTrue(generated.fields.contains("(new StringField('TITLE'))\n                ->configureRequired()\n                ->addValidator(new LengthValidator(null, 255)),"))
        assertTrue(generated.fields.contains("(new TextField('DESCRIPTION'))\n                ->configureNullable(),"))
        assertTrue(generated.fields.contains("->configureValues('N', 'Y')"))
        assertTrue(generated.fields.contains("->configureDefaultValue('Y')"))
        assertTrue(generated.fields.contains("->configureValues(['N', 'Y'])"))
    }

    @Test
    fun generatesRelations() {
        val model = OrmEntityModel(
            namespace = "Vendor\\Module\\Orm",
            className = "BookTable",
            tableName = "vendor_module_book",
            fields = listOf(
                OrmField("ID", OrmFieldType.INTEGER, primary = true, autocomplete = true),
                OrmField("AUTHOR_ID", OrmFieldType.INTEGER),
            ),
            relations = listOf(
                OrmRelation(
                    name = "AUTHOR",
                    type = OrmRelationType.REFERENCE,
                    targetEntity = "Bitrix\\Main\\UserTable",
                    localField = "AUTHOR_ID",
                    remoteField = "ID",
                ),
                OrmRelation(
                    name = "ITEMS",
                    type = OrmRelationType.ONE_TO_MANY,
                    targetEntity = "ItemTable",
                    referenceField = "BOOK",
                ),
                OrmRelation(
                    name = "TAGS",
                    type = OrmRelationType.MANY_TO_MANY,
                    targetEntity = "TagTable",
                    mediatorTable = "vendor_module_book_tag",
                ),
            ),
        )

        val generated = OrmEntityGenerator.generate(model)

        assertTrue(generated.imports.contains("use Bitrix\\Main\\ORM\\Fields\\Relations\\Reference;"))
        assertTrue(generated.imports.contains("use Bitrix\\Main\\ORM\\Fields\\Relations\\OneToMany;"))
        assertTrue(generated.imports.contains("use Bitrix\\Main\\ORM\\Fields\\Relations\\ManyToMany;"))
        assertTrue(generated.imports.contains("use Bitrix\\Main\\ORM\\Query\\Join;"))

        assertTrue(
            generated.fields.contains(
                "new Reference('AUTHOR', \\Bitrix\\Main\\UserTable::class, " +
                    "Join::on('this.AUTHOR_ID', 'ref.ID'))",
            ),
        )
        assertTrue(generated.fields.contains("new OneToMany('ITEMS', ItemTable::class, 'BOOK')"))
        assertTrue(
            generated.fields.contains(
                "(new ManyToMany('TAGS', TagTable::class))\n" +
                    "                ->configureTableName('vendor_module_book_tag'),",
            ),
        )
    }

    @Test
    fun quotesStringDefaults() {
        val model = OrmEntityModel(
            namespace = "Vendor\\Module\\Orm",
            className = "BookTable",
            tableName = "vendor_module_book",
            fields = listOf(
                OrmField(
                    "TITLE",
                    OrmFieldType.STRING,
                    defaultValue = "It's mine",
                ),
            ),
        )

        val generated = OrmEntityGenerator.generate(model)

        assertTrue(generated.fields.contains("->configureDefaultValue('It\\'s mine')"))
    }
}
