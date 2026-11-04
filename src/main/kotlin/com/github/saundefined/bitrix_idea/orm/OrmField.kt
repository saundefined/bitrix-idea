package com.github.saundefined.bitrix_idea.orm

enum class OrmArraySerialization(val displayName: String) {
    JSON("JSON"),
    PHP("PHP"),
}

data class OrmField(
    val name: String,
    val type: OrmFieldType,
    val primary: Boolean = false,
    val autocomplete: Boolean = false,
    val required: Boolean = false,
    val nullable: Boolean = false,
    val unique: Boolean = false,
    val defaultValue: String? = null,
    val defaultNow: Boolean = false,
    val columnName: String? = null,
    val size: Int? = null,
    val length: Int? = null,
    val truncateSize: Int? = null,
    val precision: Int? = null,
    val scale: Int? = null,
    val values: List<String> = emptyList(),
    val falseValue: String = "N",
    val trueValue: String = "Y",
    val format: String? = null,
    val long: Boolean = false,
    val useTimezone: Boolean = true,
    val serialization: OrmArraySerialization = OrmArraySerialization.JSON,
    val cryptoEnabled: Boolean = true,
    val cryptoKey: String? = null,
    val secretLength: Int? = null,
    val expression: String? = null,
    val expressionFields: List<String> = emptyList(),
    val validators: List<OrmValidator> = emptyList(),
) {
    fun optionsSummary(): String {
        return buildList {
            if (primary) add("PK")
            if (autocomplete) add("AI")
            if (required) add("required")
            if (nullable) add("nullable")
            if (unique) add("unique")
            columnName?.let { add("column=$it") }
            if (type.supportsSize) size?.let { add("size=$it") }
            if (type.supportsLength) length?.let { add("length=$it") }
            if (type.supportsTruncateSize) truncateSize?.let { add("truncate=$it") }
            if (type.supportsPrecision) {
                precision?.let { add("precision=$it") }
                scale?.let { add("scale=$it") }
            }
            if (type.supportsValues && values.isNotEmpty()) add("values=${values.joinToString(",")}")
            if (type.supportsBooleanValues) add("$falseValue/$trueValue")
            if (type.supportsFormat) format?.let { add("format=$it") }
            if (type.supportsLong && long) add("long")
            if (type.supportsTimezone && !useTimezone) add("no-timezone")
            if (type.supportsSerialization) add(serialization.displayName)
            if (type.supportsCrypto && !cryptoEnabled) add("crypto-off")
            if (type.supportsSecret) secretLength?.let { add("secret=$it") }
            if (type.supportsExpression) expression?.let { add("expr=$it") }
            if (validators.isNotEmpty()) add("validators=${validators.size}")
        }.joinToString(", ")
    }
}
