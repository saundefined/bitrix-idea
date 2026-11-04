package com.github.saundefined.bitrix_idea.orm

enum class OrmFieldType(
    val displayName: String,
    val phpClass: String,
    val supportsPrimary: Boolean = true,
    val supportsAutocomplete: Boolean = false,
    val supportsRequired: Boolean = true,
    val supportsNullable: Boolean = true,
    val supportsUnique: Boolean = true,
    val supportsSize: Boolean = false,
    val supportsLength: Boolean = false,
    val supportsTruncateSize: Boolean = false,
    val supportsPrecision: Boolean = false,
    val supportsValues: Boolean = false,
    val supportsBooleanValues: Boolean = false,
    val supportsDefault: Boolean = true,
    val supportsDefaultNow: Boolean = false,
    val supportsFormat: Boolean = false,
    val supportsLong: Boolean = false,
    val supportsTimezone: Boolean = false,
    val supportsSerialization: Boolean = false,
    val supportsCrypto: Boolean = false,
    val supportsSecret: Boolean = false,
    val supportsExpression: Boolean = false,
) {
    INTEGER("Integer", "IntegerField", supportsAutocomplete = true, supportsSize = true),
    FLOAT("Float", "FloatField", supportsPrecision = true),
    DECIMAL("Decimal", "DecimalField", supportsPrecision = true),
    STRING(
        "String",
        "StringField",
        supportsLength = true,
        supportsTruncateSize = true,
        supportsFormat = true,
    ),
    TEXT("Text", "TextField", supportsLong = true),
    DATE("Date", "DateField", supportsFormat = true),
    DATETIME(
        "Datetime",
        "DatetimeField",
        supportsDefaultNow = true,
        supportsFormat = true,
        supportsTimezone = true,
    ),
    BOOLEAN("Boolean", "BooleanField", supportsBooleanValues = true),
    ENUM("Enum", "EnumField", supportsValues = true),
    ARRAY("Array", "ArrayField", supportsPrimary = false, supportsUnique = false, supportsDefault = false, supportsSerialization = true),
    JSON("Json", "JsonField", supportsPrimary = false, supportsUnique = false, supportsDefault = false),
    CRYPTO("Crypto", "CryptoField", supportsPrimary = false, supportsUnique = false, supportsDefault = false, supportsCrypto = true),
    SECRET("Secret", "SecretField", supportsPrimary = false, supportsUnique = false, supportsDefault = false, supportsSecret = true),
    EXPRESSION(
        "Expression",
        "ExpressionField",
        supportsPrimary = false,
        supportsAutocomplete = false,
        supportsRequired = false,
        supportsNullable = false,
        supportsUnique = false,
        supportsDefault = false,
        supportsExpression = true,
    );

    val fqcn: String
        get() = "Bitrix\\Main\\ORM\\Fields\\$phpClass"

    fun supportsAutocompleteFor(primary: Boolean): Boolean = supportsAutocomplete && primary
}
