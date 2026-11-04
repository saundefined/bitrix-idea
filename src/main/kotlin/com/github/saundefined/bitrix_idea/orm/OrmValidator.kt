package com.github.saundefined.bitrix_idea.orm

enum class OrmValidatorType(
    val displayName: String,
    val phpClass: String? = null,
) {
    LENGTH("LengthValidator", "LengthValidator"),
    RANGE("RangeValidator", "RangeValidator"),
    REGEXP("RegExpValidator", "RegExpValidator"),
    UNIQUE("UniqueValidator", "UniqueValidator"),
    FOREIGN("ForeignValidator", "ForeignValidator"),
    EMAIL("EmailValidator", null),
}

data class OrmValidator(
    val type: OrmValidatorType,
    val min: String? = null,
    val max: String? = null,
    val pattern: String? = null,
    val targetClass: String? = null,
    val targetField: String? = null,
) {
    fun paramsSummary(): String {
        return when (type) {
            OrmValidatorType.LENGTH, OrmValidatorType.RANGE ->
                listOfNotNull(min, max).joinToString("..").ifBlank { "—" }
            OrmValidatorType.REGEXP -> pattern.orEmpty()
            OrmValidatorType.UNIQUE -> "—"
            OrmValidatorType.FOREIGN ->
                listOfNotNull(targetClass, targetField).joinToString("::").ifBlank { "—" }
            OrmValidatorType.EMAIL -> "strict"
        }
    }
}
