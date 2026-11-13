package com.github.saundefined.bitrix_idea.php

import com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
import com.intellij.openapi.project.Project

object BitrixLangMessages {
    fun value(project: Project, key: String, preferredLanguage: String? = null): String? {
        val entries = BitrixProjectStructure.findLangEntries(project, key)
        if (entries.isEmpty()) return null

        if (!preferredLanguage.isNullOrBlank()) {
            entries.firstOrNull { languageOf(it.file.path) == preferredLanguage }?.value?.let { return it }
        }
        return entries.firstOrNull { !it.value.isNullOrBlank() }?.value
    }

    fun translations(project: Project, key: String): Map<String, String> {
        return BitrixProjectStructure.findLangEntries(project, key)
            .mapNotNull { entry ->
                val language = languageOf(entry.file.path) ?: return@mapNotNull null
                val value = entry.value ?: return@mapNotNull null
                language to value
            }
            .toMap()
    }

    private fun languageOf(path: String): String? {
        val marker = "/lang/"
        val start = path.indexOf(marker)
        if (start < 0) return null
        val end = path.indexOf('/', start + marker.length)
        if (end < 0) return null
        return path.substring(start + marker.length, end)
    }
}
