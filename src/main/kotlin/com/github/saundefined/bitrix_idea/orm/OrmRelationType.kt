package com.github.saundefined.bitrix_idea.orm

enum class OrmRelationType(val displayName: String) {
    REFERENCE("Reference"),
    ONE_TO_MANY("OneToMany"),
    MANY_TO_MANY("ManyToMany"),
}
