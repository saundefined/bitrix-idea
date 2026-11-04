package com.github.saundefined.bitrix_idea.orm

object OrmInstallerGenerator {
    fun entityEntries(model: OrmEntityModel): List<String> {
        val table = "\\${model.namespace}\\${model.className}"
        val entries = mutableListOf("$table::getEntity()")
        model.relations
            .filter { it.type == OrmRelationType.MANY_TO_MANY }
            .forEach { relation ->
                entries += "$table::getEntity()->getField('${relation.name}')->getMediatorEntity()"
            }
        return entries
    }

    fun annotationBlock(model: OrmEntityModel, relativePath: String): String {
        val fqn = "${model.namespace}\\${model.className}"
        val objectClass = model.objectClassName
        val collectionClass = model.collectionClassName
        val methods = buildList {
            model.fields.forEach { field ->
                val suffix = camel(field.name)
                val type = phpType(field)
                add("@method $type get$suffix()")
                if (field.type != OrmFieldType.EXPRESSION) {
                    add("@method \\${model.namespace}\\$objectClass set$suffix($type|\\Bitrix\\Main\\DB\\SqlExpression \\${'$'}value)")
                    add("@method void reset$suffix()")
                    add("@method void unset$suffix()")
                    add("@method void fill$suffix()")
                }
                add("@method bool isFilled$suffix()")
                add("@method bool isChanged$suffix()")
                add("@method bool has$suffix()")
                add("@method mixed remindActual$suffix()")
            }
            model.relations.forEach { relation ->
                val suffix = camel(relation.name)
                val objectType = targetObjectClass(relation.targetEntity, model.namespace)
                when (relation.type) {
                    OrmRelationType.REFERENCE -> {
                        add("@method \\$objectType get$suffix()")
                        add("@method void set$suffix(\\$objectType \\${'$'}value)")
                    }
                    OrmRelationType.ONE_TO_MANY, OrmRelationType.MANY_TO_MANY -> {
                        add("@method \\${objectType}Collection get$suffix()")
                        add("@method void addTo$suffix(\\$objectType \\${'$'}value)")
                        add("@method void removeFrom$suffix(\\$objectType \\${'$'}value)")
                        add("@method void removeAll$suffix()")
                    }
                }
            }
        }.joinToString("\n     * ")

        return """
            |/* ORMENTITYANNOTATION:$fqn:$relativePath */
            |namespace ${model.namespace} {
            |    /**
            |     * EO_$objectClass
            |     * @see \$fqn
            |     *
            |     * $methods
            |     */
            |    class EO_$objectClass extends \Bitrix\Main\ORM\Objectify\EntityObject
            |    {
            |        static public ${'$'}dataClass = '\$fqn';
            |    }
            |
            |    /**
            |     * @method \${model.namespace}\$objectClass getByPrimary(${'$'}primary, array ${'$'}parameters = [])
            |     * @method \${model.namespace}\$objectClass[] getAll()
            |     * @method void add(\${model.namespace}\$objectClass ${'$'}object)
            |     * @method void remove(\${model.namespace}\$objectClass ${'$'}object)
            |     * @method bool has(\${model.namespace}\$objectClass ${'$'}object)
            |     * @method bool hasByPrimary(${'$'}primary)
            |     * @method bool isEmpty()
            |     * @method int count()
            |     * @method \Bitrix\Main\ORM\Data\Result save(${'$'}ignoreEvents = false)
            |     */
            |    class EO_${objectClass}_Collection extends \Bitrix\Main\ORM\Objectify\Collection
            |    {
            |        static public ${'$'}dataClass = '\$fqn';
            |    }
            |
            |    class EO_${objectClass}_Query extends \Bitrix\Main\ORM\Query\Query {}
            |    class EO_${objectClass}_Result extends \Bitrix\Main\ORM\Query\Result {}
            |    class EO_${objectClass}_Entity extends \Bitrix\Main\ORM\Entity {}
            |
            |    /**
            |     * @method static EO_${objectClass}_Query query()
            |     * @method static EO_${objectClass}_Result getByPrimary(${'$'}primary, array ${'$'}parameters = [])
            |     * @method static EO_${objectClass}_Result getById(${'$'}id)
            |     * @method static EO_${objectClass}_Result getList(array ${'$'}parameters = [])
            |     * @method static EO_${objectClass}_Entity getEntity()
            |     * @method static \${model.namespace}\$objectClass createObject(${'$'}setDefaultValues = true)
            |     * @method static \${model.namespace}\$collectionClass createCollection()
            |     * @method static \${model.namespace}\$objectClass wakeUpObject(${'$'}row)
            |     * @method static \${model.namespace}\$collectionClass wakeUpCollection(${'$'}rows)
            |     */
            |    class ${model.className} extends \Bitrix\Main\ORM\Data\DataManager {}
            |}
            |""".trimMargin()
    }

    private fun phpType(field: OrmField): String {
        return when (field.type) {
            OrmFieldType.INTEGER -> "int"
            OrmFieldType.FLOAT, OrmFieldType.DECIMAL -> "float"
            OrmFieldType.BOOLEAN -> "bool"
            OrmFieldType.DATE -> "\\Bitrix\\Main\\Type\\Date"
            OrmFieldType.DATETIME -> "\\Bitrix\\Main\\Type\\DateTime"
            OrmFieldType.ARRAY, OrmFieldType.JSON -> "array"
            OrmFieldType.EXPRESSION -> "mixed"
            else -> "string"
        }
    }

    private fun targetObjectClass(target: String, modelNamespace: String): String {
        val normalized = target.trim().removePrefix("\\")
        val namespace = normalized.substringBeforeLast('\\', "")
        val short = normalized.substringAfterLast('\\').removeSuffix("Table")
        return when {
            namespace.isNotEmpty() -> "$namespace\\$short"
            modelNamespace.isNotEmpty() -> "$modelNamespace\\$short"
            else -> short
        }
    }

    private fun camel(name: String): String = name.split('_')
        .filter { it.isNotEmpty() }
        .joinToString("") { it.lowercase().replaceFirstChar { c -> c.titlecase() } }
}
