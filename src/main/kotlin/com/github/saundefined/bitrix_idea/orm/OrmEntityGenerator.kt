package com.github.saundefined.bitrix_idea.orm

data class GeneratedOrmEntity(
    val imports: String,
    val fields: String,
    val fullStackMethods: String = "",
    val extraMethods: String = "",
    val eventHandlers: String = "",
    val traits: String = "",
)

object OrmEntityGenerator {
    fun generate(model: OrmEntityModel, fullStack: Boolean = true): GeneratedOrmEntity {
        val imports = buildList {
            add("Bitrix\\Main\\ORM\\Data\\DataManager")
            model.fields.map { it.type.fqcn }.distinct().sorted().forEach { add(it) }
            model.relations.map { it.type }.distinct().forEach { relationType ->
                add(
                    when (relationType) {
                        OrmRelationType.REFERENCE -> "Bitrix\\Main\\ORM\\Fields\\Relations\\Reference"
                        OrmRelationType.ONE_TO_MANY -> "Bitrix\\Main\\ORM\\Fields\\Relations\\OneToMany"
                        OrmRelationType.MANY_TO_MANY -> "Bitrix\\Main\\ORM\\Fields\\Relations\\ManyToMany"
                    },
                )
            }
            if (model.relations.any { it.type == OrmRelationType.REFERENCE }) {
                add("Bitrix\\Main\\ORM\\Query\\Join")
            }
            usedValidatorTypes(model).forEach { add("Bitrix\\Main\\ORM\\Fields\\Validators\\${it.phpClass}") }
            if (model.generateEvents) {
                add("Bitrix\\Main\\ORM\\Event")
                add("Bitrix\\Main\\ORM\\EventResult")
            }
            if (model.deleteByFilter) {
                add("Bitrix\\Main\\ORM\\Data\\Internal\\DeleteByFilterTrait")
            }
        }.joinToString("\n") { "use $it;" }

        val fields = (model.fields.map { renderField(it) } + model.relations.map { renderRelation(it) })
            .joinToString("\n")

        val fullStackMethods = if (fullStack) {
            """
            |    public static function getObjectClass(): string
            |    {
            |        return ${model.objectClassName}::class;
            |    }
            |
            |    public static function getCollectionClass(): string
            |    {
            |        return ${model.collectionClassName}::class;
            |    }
            |""".trimMargin()
        } else {
            ""
        }

        val extraMethods = buildList {
            model.ufId?.takeIf { it.isNotBlank() }?.let {
                add(
                    """
                    |    public static function getUfId(): string
                    |    {
                    |        return ${quote(it)};
                    |    }
                    |""".trimMargin(),
                )
            }
            if (!model.cacheable) {
                add(
                    """
                    |    public static function isCacheable(): bool
                    |    {
                    |        return false;
                    |    }
                    |""".trimMargin(),
                )
            }
        }.joinToString("\n")

        val eventHandlers = if (model.generateEvents) {
            """
            |    public static function onBeforeAdd(Event ${'$'}event): EventResult
            |    {
            |        return new EventResult();
            |    }
            |
            |    public static function onBeforeUpdate(Event ${'$'}event): EventResult
            |    {
            |        return new EventResult();
            |    }
            |
            |    public static function onBeforeDelete(Event ${'$'}event): EventResult
            |    {
            |        return new EventResult();
            |    }
            |""".trimMargin()
        } else {
            ""
        }

        val traits = if (model.deleteByFilter) "    use DeleteByFilterTrait;\n" else ""

        return GeneratedOrmEntity(
            imports = imports,
            fields = fields,
            fullStackMethods = fullStackMethods,
            extraMethods = extraMethods,
            eventHandlers = eventHandlers,
            traits = traits,
        )
    }

    fun generateObjectClass(model: OrmEntityModel): String {
        return """
            |<?php
            |
            |namespace ${model.namespace};
            |
            |class ${model.objectClassName} extends EO_${model.objectClassName}
            |{
            |}
            |""".trimMargin()
    }

    fun generateCollectionClass(model: OrmEntityModel): String {
        return """
            |<?php
            |
            |namespace ${model.namespace};
            |
            |class ${model.collectionClassName} extends EO_${model.objectClassName}_Collection
            |{
            |}
            |""".trimMargin()
    }

    private fun renderRelation(relation: OrmRelation): String {
        return when (relation.type) {
            OrmRelationType.REFERENCE -> {
                val target = renderTargetClass(relation.targetEntity)
                val join = "Join::on(${quote("this.${relation.localField}")}, ${quote("ref.${relation.remoteField}")})"
                val chain = mutableListOf<String>()
                if (relation.joinType != "left") chain += "configureJoinType(${quote(relation.joinType)})"
                renderChain("new Reference(${quote(relation.name)}, $target, $join)", chain)
            }

            OrmRelationType.ONE_TO_MANY -> {
                val target = renderTargetClass(relation.targetEntity)
                val chain = mutableListOf<String>()
                if (relation.joinType != "left") chain += "configureJoinType(${quote(relation.joinType)})"
                renderChain(
                    "new OneToMany(${quote(relation.name)}, $target, ${quote(relation.referenceField)})",
                    chain,
                )
            }

            OrmRelationType.MANY_TO_MANY -> {
                val target = renderTargetClass(relation.targetEntity)
                val chain = mutableListOf(
                    "configureTableName(${quote(relation.mediatorTable)})",
                )
                if (relation.joinType != "left") chain += "configureJoinType(${quote(relation.joinType)})"
                if (relation.localPrimary != "ID" || relation.localPrimaryColumn.isNotBlank()) {
                    chain += "configureLocalPrimary(${quote(relation.localPrimary)}, ${quote(relation.localPrimaryColumn.ifBlank { defaultLocalColumn(relation) })})"
                }
                if (relation.localReference.isNotBlank()) {
                    chain += "configureLocalReference(${quote(relation.localReference)})"
                }
                if (relation.remotePrimary != "ID" || relation.remotePrimaryColumn.isNotBlank()) {
                    chain += "configureRemotePrimary(${quote(relation.remotePrimary)}, ${quote(relation.remotePrimaryColumn.ifBlank { defaultRemoteColumn(relation) })})"
                }
                if (relation.remoteReference.isNotBlank()) {
                    chain += "configureRemoteReference(${quote(relation.remoteReference)})"
                }
                renderChain("new ManyToMany(${quote(relation.name)}, $target)", chain)
            }
        }
    }

    private fun renderChain(constructor: String, chain: List<String>): String {
        if (chain.isEmpty()) return "            $constructor,"
        return buildString {
            append("            ($constructor)\n")
            chain.forEachIndexed { index, call ->
                append("                ->$call")
                if (index == chain.lastIndex) append(',')
                append('\n')
            }
        }
    }

    private fun renderField(field: OrmField): String {
        if (field.type == OrmFieldType.EXPRESSION) {
            val fields = field.expressionFields.joinToString(", ") { quote(it) }
            return "            new ExpressionField(${quote(field.name)}, ${quote(field.expression.orEmpty())}, [$fields]),"
        }

        val constructor = when (field.type) {
            OrmFieldType.CRYPTO -> {
                val options = mutableListOf(
                    "'crypto_enabled' => ${if (field.cryptoEnabled) "true" else "false"}",
                )
                field.cryptoKey?.takeIf { it.isNotBlank() }?.let { options += "'crypto_key' => ${quote(it)}" }
                "new CryptoField(${quote(field.name)}, [${options.joinToString(", ")}])"
            }

            OrmFieldType.SECRET -> {
                val length = field.secretLength?.toString() ?: "null"
                "new SecretField(${quote(field.name)}, ['secret_length' => $length])"
            }

            else -> "new ${field.type.phpClass}(${quote(field.name)})"
        }

        val chain = mutableListOf<String>()
        if (field.primary) chain += "configurePrimary()"
        if (field.autocomplete && field.primary) chain += "configureAutocomplete()"
        if (field.required) chain += "configureRequired()"
        if (field.nullable) chain += "configureNullable()"
        if (field.unique) chain += "configureUnique()"
        field.columnName?.takeIf { it.isNotBlank() }?.let { chain += "configureColumnName(${quote(it)})" }

        if (field.type == OrmFieldType.INTEGER) {
            field.size?.let { chain += "configureSize($it)" }
        }
        if (field.type == OrmFieldType.STRING) {
            field.truncateSize?.let { chain += "configureSize($it)" }
        }
        if (field.type.supportsPrecision) {
            field.precision?.let { chain += "configurePrecision($it)" }
            field.scale?.let { chain += "configureScale($it)" }
        }
        if (field.type == OrmFieldType.BOOLEAN) {
            chain += "configureValues(${quote(field.falseValue)}, ${quote(field.trueValue)})"
        }
        if (field.type == OrmFieldType.ENUM) {
            chain += "configureValues([${field.values.joinToString(", ") { quote(it) }}])"
        }
        if (field.type.supportsFormat) {
            field.format?.takeIf { it.isNotBlank() }?.let { chain += "configureFormat(${quote(it)})" }
        }
        if (field.type.supportsLong && field.long) chain += "configureLong()"
        if (field.type.supportsTimezone && !field.useTimezone) chain += "configureUseTimezone(false)"
        if (field.type.supportsSerialization) {
            chain += when (field.serialization) {
                OrmArraySerialization.JSON -> "configureSerializationJson()"
                OrmArraySerialization.PHP -> "configureSerializationPhp()"
            }
        }
        if (field.type.supportsDefault && !field.defaultNow) {
            field.defaultValue?.takeIf { it.isNotBlank() }?.let {
                chain += "configureDefaultValue(${renderDefault(field, it)})"
            }
        }
        if (field.type.supportsDefaultNow && field.defaultNow) {
            chain += "configureDefaultValueNow()"
        }

        val validators = field.validators.toMutableList()
        if (field.type == OrmFieldType.STRING && field.length != null && validators.none { it.type == OrmValidatorType.LENGTH }) {
            validators += OrmValidator(OrmValidatorType.LENGTH, max = field.length.toString())
        }
        validators.forEach { chain += "addValidator(${renderValidator(it)})" }

        if (chain.isEmpty()) return "            $constructor,"

        return buildString {
            append("            ($constructor)\n")
            chain.forEachIndexed { index, call ->
                append("                ->$call")
                if (index == chain.lastIndex) append(',')
                append('\n')
            }
        }
    }

    private fun renderDefault(field: OrmField, value: String): String {
        return when (field.type) {
            OrmFieldType.INTEGER ->
                if (value.matches(Regex("^-?\\d+$"))) value else quote(value)
            OrmFieldType.FLOAT, OrmFieldType.DECIMAL ->
                if (value.matches(Regex("^-?\\d+(\\.\\d+)?$"))) value else quote(value)
            OrmFieldType.BOOLEAN ->
                if (value.lowercase() in setOf("1", "true", "y")) quote(field.trueValue) else quote(field.falseValue)
            OrmFieldType.DATE -> "new \\Bitrix\\Main\\Type\\Date(${quote(value)}, 'Y-m-d')"
            OrmFieldType.DATETIME -> "new \\Bitrix\\Main\\Type\\DateTime(${quote(value)}, 'Y-m-d H:i:s')"
            else -> quote(value)
        }
    }

    private fun renderValidator(validator: OrmValidator): String {
        return when (validator.type) {
            OrmValidatorType.LENGTH ->
                "new LengthValidator(${renderNumberOrNull(validator.min)}, ${renderNumberOrNull(validator.max)})"
            OrmValidatorType.RANGE ->
                "new RangeValidator(${renderNumberOrNull(validator.min)}, ${renderNumberOrNull(validator.max)})"
            OrmValidatorType.REGEXP -> "new RegExpValidator(${quote(validator.pattern.orEmpty())})"
            OrmValidatorType.UNIQUE -> "new UniqueValidator()"
            OrmValidatorType.FOREIGN -> {
                val target = renderClassConstant(validator.targetClass.orEmpty())
                "new ForeignValidator($target::getEntity()->getField(${quote(validator.targetField.orEmpty())}))"
            }
            OrmValidatorType.EMAIL ->
                """
                |static function (${'$'}value) {
                |            ${'$'}result = (new \Bitrix\Main\Validation\Validator\EmailValidator(strict: true))->validate(${'$'}value);
                |            return ${'$'}result->isSuccess() ? true : implode('; ', ${'$'}result->getErrorMessages());
                |        }
                |""".trimMargin()
        }
    }

    private fun renderNumberOrNull(value: String?): String {
        val text = value?.trim().orEmpty()
        if (text.isEmpty()) return "null"
        return if (text.matches(Regex("^-?\\d+(\\.\\d+)?$"))) text else quote(text)
    }

    private fun usedValidatorTypes(model: OrmEntityModel): Set<OrmValidatorType> {
        return buildSet {
            model.fields.flatMap { it.validators }.mapTo(this) { it.type }
            if (model.fields.any { it.type == OrmFieldType.STRING && it.length != null && it.validators.none { v -> v.type == OrmValidatorType.LENGTH } }) {
                add(OrmValidatorType.LENGTH)
            }
        }.filter { it.phpClass != null }.toSet()
    }

    private fun renderTargetClass(value: String): String = renderClassConstant(value) + "::class"

    private fun renderClassConstant(value: String): String {
        val normalized = value.trim().removePrefix("\\")
        return if (normalized.contains('\\')) "\\$normalized" else normalized
    }

    private fun normalizeClass(value: String): String = value.trim().removePrefix("\\")

    private fun defaultLocalColumn(relation: OrmRelation): String = relation.localPrimary

    private fun defaultRemoteColumn(relation: OrmRelation): String = relation.remotePrimary

    private fun quote(value: String): String {
        val escaped = value.replace("\\", "\\\\").replace("'", "\\'")
        return "'$escaped'"
    }
}
