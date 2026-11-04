package com.github.saundefined.bitrix_idea.orm

data class OrmRelation(
    val name: String,
    val type: OrmRelationType,
    val targetEntity: String,
    val localField: String = "",
    val remoteField: String = "ID",
    val referenceField: String = "",
    val mediatorTable: String = "",
    val joinType: String = "left",
    val localPrimary: String = "ID",
    val localPrimaryColumn: String = "",
    val remotePrimary: String = "ID",
    val remotePrimaryColumn: String = "",
    val localReference: String = "",
    val remoteReference: String = "",
) {
    fun optionsSummary(): String {
        return when (type) {
            OrmRelationType.REFERENCE ->
                "local=${localField.ifBlank { "?" }}, remote=${remoteField.ifBlank { "?" }}, join=$joinType"
            OrmRelationType.ONE_TO_MANY ->
                "reference=${referenceField.ifBlank { "?" }}, join=$joinType"
            OrmRelationType.MANY_TO_MANY ->
                "mediator=${mediatorTable.ifBlank { "?" }}, join=$joinType"
        }
    }
}
