package com.github.saundefined.bitrix_idea.orm

object OrmEntityValidator {
    const val ERROR_NAMESPACE = "create.orm.entity.validation.namespace"
    const val ERROR_CLASS = "create.orm.entity.validation.class"
    const val ERROR_TABLE = "create.orm.entity.validation.table"
    const val ERROR_FIELDS_EMPTY = "create.orm.entity.validation.fields.empty"
    const val ERROR_FIELD_NAME = "create.orm.entity.validation.field.name"
    const val ERROR_FIELD_DUPLICATE = "create.orm.entity.validation.field.duplicate"
    const val ERROR_FIELD_REQUIRED_NULLABLE = "create.orm.entity.validation.field.required.nullable"
    const val ERROR_FIELD_AUTOCOMPLETE = "create.orm.entity.validation.field.autocomplete"
    const val ERROR_FIELD_PRIMARY = "create.orm.entity.validation.field.primary"
    const val ERROR_FIELD_VALUES = "create.orm.entity.validation.field.values"
    const val ERROR_FIELD_SIZE = "create.orm.entity.validation.field.size"
    const val ERROR_FIELD_LENGTH = "create.orm.entity.validation.field.length"
    const val ERROR_FIELD_PRECISION = "create.orm.entity.validation.field.precision"
    const val ERROR_FIELD_DEFAULT = "create.orm.entity.validation.field.default"
    const val ERROR_FIELD_DEFAULT_UNSUPPORTED = "create.orm.entity.validation.field.default.unsupported"
    const val ERROR_FIELD_COLUMN = "create.orm.entity.validation.field.column"
    const val ERROR_FIELD_EXPRESSION = "create.orm.entity.validation.field.expression"
    const val ERROR_FIELD_EXPRESSION_OPTIONS = "create.orm.entity.validation.field.expression.options"
    const val ERROR_FIELD_BOOLEAN_VALUES = "create.orm.entity.validation.field.boolean.values"
    const val ERROR_FIELD_SECRET = "create.orm.entity.validation.field.secret"
    const val ERROR_VALIDATOR = "create.orm.entity.validation.validator"
    const val ERROR_VALIDATOR_RANGE = "create.orm.entity.validation.validator.range"
    const val ERROR_VALIDATOR_REGEXP = "create.orm.entity.validation.validator.regexp"
    const val ERROR_VALIDATOR_FOREIGN = "create.orm.entity.validation.validator.foreign"
    const val ERROR_PRIMARY_MISSING = "create.orm.entity.validation.primary.missing"
    const val ERROR_RELATION_NAME = "create.orm.entity.validation.relation.name"
    const val ERROR_RELATION_DUPLICATE = "create.orm.entity.validation.relation.duplicate"
    const val ERROR_RELATION_TARGET = "create.orm.entity.validation.relation.target"
    const val ERROR_RELATION_LOCAL_FIELD = "create.orm.entity.validation.relation.local.field"
    const val ERROR_RELATION_LOCAL_FIELD_MISSING = "create.orm.entity.validation.relation.local.field.missing"
    const val ERROR_RELATION_REMOTE_FIELD = "create.orm.entity.validation.relation.remote.field"
    const val ERROR_RELATION_REFERENCE_FIELD = "create.orm.entity.validation.relation.reference.field"
    const val ERROR_RELATION_MEDIATOR = "create.orm.entity.validation.relation.mediator"
    const val ERROR_RELATION_JOIN = "create.orm.entity.validation.relation.join"

    private val NAMESPACE = Regex("^[A-Za-z_][A-Za-z0-9_]*(\\\\[A-Za-z_][A-Za-z0-9_]*)*$")
    private val CLASS_NAME = Regex("^[A-Z][A-Za-z0-9]*$")
    private val TABLE_NAME = Regex("^[a-z][a-z0-9_]*$")
    private val COLUMN_NAME = Regex("^[A-Za-z_][A-Za-z0-9_]*$")
    private val FIELD_NAME = Regex("^[A-Z][A-Z0-9_]*$")
    private val TARGET_ENTITY = Regex("^[A-Za-z_][A-Za-z0-9_]*(\\\\[A-Za-z_][A-Za-z0-9_]*)*$")
    private val INTEGER = Regex("^-?\\d+$")
    private val DECIMAL = Regex("^-?\\d+(\\.\\d+)?$")

    fun namespaceError(value: String): String? =
        if (NAMESPACE.matches(value)) null else ERROR_NAMESPACE

    fun classNameError(value: String): String? {
        val normalized = OrmNameUtils.normalizeClassName(value)
        if (normalized.isEmpty() || normalized == "Table") return ERROR_CLASS
        return if (CLASS_NAME.matches(normalized)) null else ERROR_CLASS
    }

    fun tableNameError(value: String): String? {
        if (value.isBlank()) return null
        return if (TABLE_NAME.matches(value) && value.length <= 64) null else ERROR_TABLE
    }

    fun fieldError(field: OrmField, existingNames: Set<String> = emptySet()): String? {
        if (!FIELD_NAME.matches(field.name)) return ERROR_FIELD_NAME
        if (field.name in existingNames) return ERROR_FIELD_DUPLICATE
        if (field.required && field.nullable) return ERROR_FIELD_REQUIRED_NULLABLE
        if (field.autocomplete && (!field.primary || !field.type.supportsAutocomplete)) {
            return ERROR_FIELD_AUTOCOMPLETE
        }
        if (field.primary && !field.type.supportsPrimary) return ERROR_FIELD_PRIMARY
        if (field.nullable && !field.type.supportsNullable) return ERROR_FIELD_PRIMARY
        if (field.required && !field.type.supportsRequired) return ERROR_FIELD_PRIMARY
        if (!field.type.supportsUnique && field.unique) return ERROR_FIELD_PRIMARY

        field.columnName?.takeIf { it.isNotBlank() }?.let {
            if (!COLUMN_NAME.matches(it)) return ERROR_FIELD_COLUMN
        }

        if (field.type == OrmFieldType.EXPRESSION) {
            if (field.expression.isNullOrBlank()) return ERROR_FIELD_EXPRESSION
            if (field.primary || field.autocomplete || field.required || field.nullable || field.unique) {
                return ERROR_FIELD_EXPRESSION_OPTIONS
            }
        }

        if (field.type.supportsValues && field.values.isEmpty()) return ERROR_FIELD_VALUES
        if (field.type.supportsBooleanValues &&
            (field.falseValue.isBlank() || field.trueValue.isBlank() || field.falseValue == field.trueValue)
        ) {
            return ERROR_FIELD_BOOLEAN_VALUES
        }
        if (field.type.supportsSecret && (field.secretLength == null || field.secretLength <= 0)) {
            return ERROR_FIELD_SECRET
        }

        if (field.type.supportsSize && field.size != null && field.size !in setOf(2, 4, 8)) {
            return ERROR_FIELD_SIZE
        }
        if (field.type.supportsLength && field.length != null && field.length <= 0) {
            return ERROR_FIELD_LENGTH
        }
        if (field.type.supportsTruncateSize && field.truncateSize != null && field.truncateSize <= 0) {
            return ERROR_FIELD_LENGTH
        }

        if (field.type.supportsPrecision) {
            val precision = field.precision
            val scale = field.scale
            if ((precision != null && precision < 0) || (scale != null && scale < 0)) {
                return ERROR_FIELD_PRECISION
            }
            if (precision != null && scale != null && scale > precision) {
                return ERROR_FIELD_PRECISION
            }
        }

        field.defaultValue?.takeIf { it.isNotBlank() }?.let { value ->
            if (!field.type.supportsDefault) return ERROR_FIELD_DEFAULT_UNSUPPORTED
            val valid = when (field.type) {
                OrmFieldType.INTEGER -> INTEGER.matches(value)
                OrmFieldType.FLOAT, OrmFieldType.DECIMAL -> DECIMAL.matches(value)
                OrmFieldType.BOOLEAN -> value.lowercase() in setOf("1", "0", "true", "false", "y", "n")
                OrmFieldType.ENUM -> field.values.isEmpty() || value in field.values
                else -> true
            }
            if (!valid) return ERROR_FIELD_DEFAULT
        }

        field.validators.forEach { validator ->
            validatorError(validator)?.let { return it }
        }

        return null
    }

    fun validatorError(validator: OrmValidator): String? {
        return when (validator.type) {
            OrmValidatorType.LENGTH, OrmValidatorType.RANGE -> {
                val min = validator.min?.trim().orEmpty().toDoubleOrNull()
                val max = validator.max?.trim().orEmpty().toDoubleOrNull()
                if (validator.min?.isNotBlank() == true && min == null) return ERROR_VALIDATOR_RANGE
                if (validator.max?.isNotBlank() == true && max == null) return ERROR_VALIDATOR_RANGE
                if (min != null && max != null && min > max) ERROR_VALIDATOR_RANGE else null
            }
            OrmValidatorType.REGEXP ->
                if (validator.pattern.isNullOrBlank()) ERROR_VALIDATOR_REGEXP else null
            OrmValidatorType.FOREIGN ->
                if (validator.targetClass.isNullOrBlank() || validator.targetField.isNullOrBlank()) {
                    ERROR_VALIDATOR_FOREIGN
                } else {
                    null
                }
            OrmValidatorType.UNIQUE, OrmValidatorType.EMAIL -> null
        }
    }

    fun relationError(
        relation: OrmRelation,
        existingNames: Set<String> = emptySet(),
        scalarFieldNames: Set<String> = emptySet(),
    ): String? {
        if (!FIELD_NAME.matches(relation.name)) return ERROR_RELATION_NAME
        if (relation.name in existingNames) return ERROR_RELATION_DUPLICATE
        if (relation.joinType !in setOf("left", "inner", "right")) return ERROR_RELATION_JOIN

        val target = relation.targetEntity.trim().removePrefix("\\")
        if (target.isEmpty() || !TARGET_ENTITY.matches(target)) return ERROR_RELATION_TARGET

        return when (relation.type) {
            OrmRelationType.REFERENCE -> {
                if (!FIELD_NAME.matches(relation.localField)) return ERROR_RELATION_LOCAL_FIELD
                if (relation.localField !in scalarFieldNames) return ERROR_RELATION_LOCAL_FIELD_MISSING
                if (!FIELD_NAME.matches(relation.remoteField)) return ERROR_RELATION_REMOTE_FIELD
                null
            }

            OrmRelationType.ONE_TO_MANY -> {
                if (!FIELD_NAME.matches(relation.referenceField)) return ERROR_RELATION_REFERENCE_FIELD
                null
            }

            OrmRelationType.MANY_TO_MANY -> {
                if (relation.mediatorTable.isBlank() || !TABLE_NAME.matches(relation.mediatorTable)) {
                    return ERROR_RELATION_MEDIATOR
                }
                if (!FIELD_NAME.matches(relation.localPrimary)) return ERROR_RELATION_LOCAL_FIELD
                if (!FIELD_NAME.matches(relation.remotePrimary)) return ERROR_RELATION_REMOTE_FIELD
                if (relation.localPrimaryColumn.isNotBlank() && !COLUMN_NAME.matches(relation.localPrimaryColumn)) {
                    return ERROR_RELATION_LOCAL_FIELD
                }
                if (relation.remotePrimaryColumn.isNotBlank() && !COLUMN_NAME.matches(relation.remotePrimaryColumn)) {
                    return ERROR_RELATION_REMOTE_FIELD
                }
                null
            }
        }
    }

    fun modelError(model: OrmEntityModel): String? {
        namespaceError(model.namespace)?.let { return it }
        classNameError(model.className)?.let { return it }
        tableNameError(model.tableName)?.let { return it }

        if (model.fields.isEmpty()) return ERROR_FIELDS_EMPTY

        val names = mutableSetOf<String>()
        model.fields.forEach { field ->
            fieldError(field, names)?.let { return it }
            names += field.name
        }

        val allNames = names.toMutableSet()
        val scalarFieldNames = model.fields.map { it.name }.toSet()
        model.relations.forEach { relation ->
            relationError(relation, allNames, scalarFieldNames)?.let { return it }
            allNames += relation.name
        }

        if (model.fields.none { it.primary }) return ERROR_PRIMARY_MISSING

        return null
    }
}
