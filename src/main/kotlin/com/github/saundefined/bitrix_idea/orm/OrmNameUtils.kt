package com.github.saundefined.bitrix_idea.orm

import java.util.Locale

object OrmNameUtils {
    fun normalizeClassName(value: String): String {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return ""
        return if (trimmed.endsWith("Table", ignoreCase = true)) {
            trimmed.dropLast(5) + "Table"
        } else {
            trimmed + "Table"
        }
    }

    fun toSnakeCase(value: String): String {
        return value
            .replace(Regex("([a-z0-9])([A-Z])"), "$1_$2")
            .replace(Regex("[^A-Za-z0-9]+"), "_")
            .trim('_')
            .lowercase(Locale.ROOT)
    }
}
