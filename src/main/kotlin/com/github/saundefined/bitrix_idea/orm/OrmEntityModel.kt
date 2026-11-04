package com.github.saundefined.bitrix_idea.orm

data class OrmEntityModel(
    val namespace: String,
    val className: String,
    val tableName: String,
    val fields: List<OrmField>,
    val relations: List<OrmRelation> = emptyList(),
    val ufId: String? = null,
    val cacheable: Boolean = true,
    val deleteByFilter: Boolean = false,
    val generateEvents: Boolean = false,
) {
    val objectClassName: String
        get() = className.removeSuffix("Table")

    val collectionClassName: String
        get() = objectClassName + "Collection"
}
