package com.github.saundefined.bitrix_idea.orm

import java.util.Locale

data class OrmEntityContext(
    val namespace: String,
    val tablePrefix: String,
    val moduleRoot: String? = null,
)

/**
 * Определяет namespace и префикс таблицы по выбранному каталогу внутри модуля Bitrix.
 * Поддерживаются `local/modules/<module>/lib[/...]` и `bitrix/modules/<module>/lib[/...]`.
 *
 * Коды модулей обрабатываются одинаково в обоих местах:
 * - `vendor.module` -> `Vendor\Module`
 * - `vendor` -> `Bitrix\Vendor`
 */
object OrmEntityContextResolver {
    fun resolvePath(path: String): OrmEntityContext {
        val segments = path.split('/').filter { it.isNotEmpty() }

        for (index in 1 until segments.size - 2) {
            if (segments[index] != "modules") continue

            val source = segments[index - 1]
            if (source != "local" && source != "bitrix") continue
            if (segments[index + 2] != "lib") continue

            val moduleCode = segments[index + 1]
            val relativePath = segments.drop(index + 3)
            val moduleParts = moduleCode
                .split('.', '_', '-')
                .filter { it.isNotEmpty() }
                .map { it.toPascalCase() }

            val namespaceParts = if (moduleParts.size == 1) listOf("Bitrix") + moduleParts else moduleParts
            val namespace = (namespaceParts + relativePath).joinToString("\\")
            val tablePrefix = moduleCode
                .lowercase(Locale.ROOT)
                .replace(Regex("[^a-z0-9]+"), "_") + "_"

            val moduleRoot = "/" + segments.take(index + 2).joinToString("/")
            return OrmEntityContext(namespace, tablePrefix, moduleRoot)
        }

        return OrmEntityContext("", "")
    }

    private fun String.toPascalCase(): String {
        return replaceFirstChar { it.titlecase(Locale.ROOT) }
    }
}
