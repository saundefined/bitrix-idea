package com.github.saundefined.bitrix_idea.php

import com.github.saundefined.bitrix_idea.library.BitrixLibraryRootsProvider
import com.github.saundefined.bitrix_idea.settings.BitrixProjectSettings
import com.intellij.openapi.project.Project

object BitrixCoreModules {
    val ids: Set<String> = setOf(
        "main", "iblock", "sale", "catalog", "crm", "blog", "forum", "socialnetwork",
        "socialservices", "search", "fileman", "form", "subscribe", "currency",
        "highloadblock", "vote", "workflow", "statistic", "support", "calendar",
        "tasks", "im", "mobileapp", "rest", "bizproc", "landing", "sender", "ui",
        "pull", "cluster", "clouds", "seo", "translate", "security", "perfmon",
        "scale", "webservice", "advertising", "abtest", "conversion", "intranet",
        "bitrix24", "b24connector", "messageservice", "notifications", "report",
        "lists", "disk", "dav", "documentgenerator", "voximplant", "timeman",
        "meeting", "wiki", "learning", "photogallery", "idea", "ldap", "map",
        "mail", "rss", "xml", "compression", "controller", "faceid", "location",
        "humanresources", "storeassist", "sign",
    )

    private val CORE_KEY_PREFIXES = setOf(
        "MAIN_", "IBLOCK_", "SALE_", "CATALOG_", "CRM_", "BLOG_", "FORUM_", "SOCNET_",
        "SEARCH_", "FILEMAN_", "FORM_", "SUBSCRIBE_", "CURRENCY_", "HLBLOCK_", "VOTE_",
        "WORKFLOW_", "STATISTIC_", "SUPPORT_", "CALENDAR_", "TASKS_", "IM_", "REST_",
        "BIZPROC_", "LANDING_", "SENDER_", "UI_", "PULL_", "CLUSTER_", "SEO_", "TRANSLATE_",
        "SECURITY_", "PERFMON_", "SCALE_", "WEBSERVICE_", "ADVERTISING_", "ABTEST_",
        "CONVERSION_", "INTRANET_", "BITRIX24_", "B24CONNECTOR_", "MESSAGESERVICE_",
        "NOTIFICATIONS_", "REPORT_", "LISTS_", "DISK_", "DAV_", "DOCUMENTGENERATOR_",
        "VOXIMPLANT_", "TIMEMAN_", "MEETING_", "WIKI_", "LEARNING_", "PHOTOGALLERY_",
        "IDEA_", "LDAP_", "MAP_", "MAIL_", "RSS_", "XML_", "COMPRESSION_", "CONTROLLER_",
        "FACEID_", "LOCATION_", "HUMANRESOURCES_", "STOREASSIST_", "SIGN_",
    )

    /**
     * Все модули ядра: известный статический список и фактический состав
     * встроенной библиотеки `bitrix-idea-library`.
     */
    fun allIds(): Set<String> = ids + BitrixLibraryRootsProvider.bundledModuleIds()

    /**
     * Модули, доступные для автодополнения с учётом настроек индексации.
     */
    fun enabledIds(project: Project): Set<String> {
        val disabled = BitrixProjectSettings.getInstance(project).disabledModules.toSet()
        return allIds() - disabled
    }

    /**
     * Фразы ядра часто используются в проектном коде, но их lang-файлы лежат
     * во встроенной библиотеке, а не в проекте. Такие ключи не считаем отсутствующими.
     */
    fun isLikelyCoreKey(key: String): Boolean = CORE_KEY_PREFIXES.any { key.startsWith(it) }
}
