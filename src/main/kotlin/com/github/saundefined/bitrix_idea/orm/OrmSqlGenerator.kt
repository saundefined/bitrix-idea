package com.github.saundefined.bitrix_idea.orm

object OrmSqlGenerator {
    fun createTable(model: OrmEntityModel): String {
        val columns = model.fields
            .filter { it.type != OrmFieldType.EXPRESSION }
            .map { column(it) }
            .toMutableList()
        val primary = model.fields.filter { it.primary && it.type != OrmFieldType.EXPRESSION }
        if (primary.isNotEmpty()) {
            columns += "PRIMARY KEY (${primary.joinToString(", ") { quote(it.name) }})"
        }
        model.fields.filter { it.unique && !it.primary && it.type != OrmFieldType.EXPRESSION }.forEach { field ->
            columns += "UNIQUE KEY ${quote("ux_${model.tableName}_${field.name.lowercase()}")} (${quote(field.name)})"
        }

        val main = buildString {
            append("CREATE TABLE IF NOT EXISTS ${quote(model.tableName)} (\n")
            columns.forEachIndexed { index, line ->
                append("  $line")
                if (index < columns.lastIndex) append(',')
                append('\n')
            }
            append(");\n")
        }

        val mediators = model.relations
            .filter { it.type == OrmRelationType.MANY_TO_MANY && it.mediatorTable.isNotBlank() }
            .joinToString("\n") { mediatorTable(model, it) }

        return listOf(main, mediators).filter { it.isNotBlank() }.joinToString("\n")
    }

    fun dropTable(model: OrmEntityModel): String {
        val tables = buildList {
            add(model.tableName)
            model.relations.filter { it.type == OrmRelationType.MANY_TO_MANY }.mapNotNullTo(this) {
                it.mediatorTable.takeIf { table -> table.isNotBlank() }
            }
        }.distinct()
        return tables.joinToString("\n") { "DROP TABLE IF EXISTS ${quote(it)};" } + "\n"
    }

    private fun mediatorTable(model: OrmEntityModel, relation: OrmRelation): String {
        val localColumn = relation.localPrimaryColumn.ifBlank {
            "${model.objectClassName.uppercase()}_${relation.localPrimary}"
        }
        val remoteColumn = relation.remotePrimaryColumn.ifBlank {
            "${targetObjectShortName(relation.targetEntity)}_${relation.remotePrimary}"
        }
        return buildString {
            append("CREATE TABLE IF NOT EXISTS ${quote(relation.mediatorTable)} (\n")
            append("  ${quote(localColumn)} int NOT NULL,\n")
            append("  ${quote(remoteColumn)} int NOT NULL,\n")
            append("  PRIMARY KEY (${quote(localColumn)}, ${quote(remoteColumn)})\n")
            append(");\n")
        }
    }

    private fun column(field: OrmField): String {
        val type = sqlType(field)
        val nullability = if (field.primary || field.required) " NOT NULL" else " NULL"
        val autoIncrement = if (field.primary && field.autocomplete) " AUTO_INCREMENT" else ""
        val default = field.defaultValue
            ?.takeIf { it.isNotBlank() && field.type.supportsDefault && !field.defaultNow }
            ?.let { " DEFAULT ${renderDefault(field, it)}" }
            .orEmpty()
        return "${quote(field.name)} $type$nullability$autoIncrement$default"
    }

    private fun sqlType(field: OrmField): String {
        return when (field.type) {
            OrmFieldType.INTEGER -> when (field.size) {
                2 -> "smallint"
                8 -> "bigint"
                else -> "int"
            }
            OrmFieldType.STRING -> {
                val validatorLength = field.validators
                    .firstOrNull { it.type == OrmValidatorType.LENGTH }
                    ?.max
                    ?.toIntOrNull()
                "varchar(${field.length ?: validatorLength ?: 255})"
            }
            OrmFieldType.TEXT -> if (field.long) "longtext" else "text"
            OrmFieldType.BOOLEAN -> "char(1)"
            OrmFieldType.DATE -> "date"
            OrmFieldType.DATETIME -> "datetime"
            OrmFieldType.FLOAT -> "float"
            OrmFieldType.DECIMAL -> "decimal(${field.precision ?: 18}, ${field.scale ?: 2})"
            OrmFieldType.ENUM -> "varchar(255)"
            OrmFieldType.ARRAY, OrmFieldType.JSON, OrmFieldType.CRYPTO, OrmFieldType.SECRET -> "text"
            OrmFieldType.EXPRESSION -> "text"
        }
    }

    private fun renderDefault(field: OrmField, value: String): String {
        return when (field.type) {
            OrmFieldType.INTEGER ->
                if (value.matches(Regex("^-?\\d+$"))) value else sqlString(value)
            OrmFieldType.FLOAT, OrmFieldType.DECIMAL ->
                if (value.matches(Regex("^-?\\d+(\\.\\d+)?$"))) value else sqlString(value)
            OrmFieldType.BOOLEAN -> when (value.lowercase()) {
                "1", "true", "y" -> sqlString(field.trueValue)
                else -> sqlString(field.falseValue)
            }
            else -> sqlString(value)
        }
    }

    private fun targetObjectShortName(target: String): String {
        val short = target.trim().removePrefix("\\").substringAfterLast('\\').removeSuffix("Table")
        return short.uppercase()
    }

    private fun quote(value: String): String = "`$value`"

    private fun sqlString(value: String): String = "'${value.replace("\\", "\\\\").replace("'", "''")}'"
}
