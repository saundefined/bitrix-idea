package com.github.saundefined.bitrix_idea.php

/**
 * Часто используемые события ядра. События проекта можно вводить вручную;
 * список нужен только для подсказки, а не для валидации.
 */
object BitrixEventNames {
    private val byModule = mapOf(
        "main" to setOf(
            "OnBeforeProlog", "OnProlog", "OnEpilog", "OnAfterEpilog", "OnEndBufferContent",
            "OnBeforeUserAdd", "OnAfterUserAdd", "OnBeforeUserUpdate", "OnAfterUserUpdate",
            "OnBeforeUserDelete", "OnAfterUserDelete", "OnUserLogin", "OnAfterUserLogin",
            "OnBeforeUserLogin", "OnUserLogout", "OnAfterUserLogout", "OnBeforeUserRegister",
            "OnAfterUserRegister", "OnBeforeUserSendPassword", "OnAfterUserSendPassword",
            "OnBeforeUserChangePassword", "OnAfterUserChangePassword", "OnBuildGlobalMenu",
            "OnAdminListDisplay", "OnAdminContextMenuShow", "OnAdminTabControlBegin",
            "OnAdminMenuBuild", "OnBeforeMailSend", "OnAfterMailSend", "OnBeforeEventSend",
            "OnAfterEventSend", "OnBeforeEventAdd", "OnAfterEventAdd", "OnBeforeEventMessageAdd",
            "OnAfterEventMessageAdd", "OnBeforeEventMessageUpdate", "OnAfterEventMessageUpdate",
            "OnBeforeEventMessageDelete", "OnAfterEventMessageDelete", "OnBeforeEventTypeAdd",
            "OnAfterEventTypeAdd", "OnBeforeEventTypeUpdate", "OnAfterEventTypeUpdate",
            "OnBeforeEventTypeDelete", "OnAfterEventTypeDelete",
        ),
        "iblock" to setOf(
            "OnBeforeIBlockAdd", "OnAfterIBlockAdd", "OnBeforeIBlockUpdate", "OnAfterIBlockUpdate",
            "OnBeforeIBlockDelete", "OnAfterIBlockDelete",
            "OnBeforeIBlockElementAdd", "OnAfterIBlockElementAdd",
            "OnBeforeIBlockElementUpdate", "OnAfterIBlockElementUpdate",
            "OnBeforeIBlockElementDelete", "OnAfterIBlockElementDelete",
            "OnBeforeIBlockSectionAdd", "OnAfterIBlockSectionAdd",
            "OnBeforeIBlockSectionUpdate", "OnAfterIBlockSectionUpdate",
            "OnBeforeIBlockSectionDelete", "OnAfterIBlockSectionDelete",
            "OnIBlockElementSetPropertyValues", "OnAfterIBlockElementSetPropertyValues",
        ),
        "sale" to setOf(
            "OnSaleOrderSaved", "OnSaleOrderBeforeSaved", "OnSaleOrderDeleted", "OnSaleOrderPaid",
            "OnSaleOrderCanceled", "OnSaleOrderAllowPayment", "OnSaleOrderBeforeDelete",
            "OnSaleStatusOrderChange", "OnSaleComponentOrderOneStepComplete",
            "OnSaleComponentOrderCreated", "OnSaleComponentOrderProps",
            "OnSaleComponentOrderUserResult", "OnSaleBasketItemRefresh",
        ),
        "catalog" to setOf(
            "OnGetOptimalPrice", "OnPriceAdd", "OnPriceUpdate", "OnPriceDelete",
            "OnProductAdd", "OnProductUpdate", "OnProductDelete",
        ),
        "crm" to setOf(
            "OnAfterCrmLeadAdd", "OnAfterCrmDealAdd", "OnAfterCrmContactAdd",
            "OnAfterCrmCompanyAdd", "OnBeforeCrmLeadAdd", "OnBeforeCrmDealAdd",
            "OnCrmEntityUpdate", "OnCrmEntityDelete",
        ),
        "highloadblock" to setOf(
            "OnBeforeHighloadblockAdd", "OnAfterHighloadblockAdd",
            "OnBeforeHighloadblockUpdate", "OnAfterHighloadblockUpdate",
            "OnBeforeHighloadblockDelete", "OnAfterHighloadblockDelete",
        ),
        "forum" to setOf(
            "OnBeforeForumAdd", "OnAfterForumAdd", "OnBeforeForumUpdate", "OnAfterForumUpdate",
            "OnBeforeForumDelete", "OnAfterForumDelete", "OnBeforeForumMessageAdd",
            "OnAfterForumMessageAdd", "OnBeforeForumMessageUpdate", "OnAfterForumMessageUpdate",
            "OnBeforeForumMessageDelete", "OnAfterForumMessageDelete",
        ),
        "blog" to setOf(
            "OnBeforeBlogAdd", "OnAfterBlogAdd", "OnBeforeBlogUpdate", "OnAfterBlogUpdate",
            "OnBeforeBlogDelete", "OnAfterBlogDelete", "OnBeforeBlogPostAdd",
            "OnAfterBlogPostAdd", "OnBeforeBlogPostUpdate", "OnAfterBlogPostUpdate",
            "OnBeforeBlogPostDelete", "OnAfterBlogPostDelete",
        ),
        "socialnetwork" to setOf(
            "OnBeforeSocNetGroupAdd", "OnAfterSocNetGroupAdd",
            "OnBeforeSocNetGroupUpdate", "OnAfterSocNetGroupUpdate",
            "OnBeforeSocNetGroupDelete", "OnAfterSocNetGroupDelete",
        ),
        "search" to setOf("OnReindex", "OnSearchGetUrl", "OnSearchGetFileContent"),
        "fileman" to setOf("OnBeforeFileAdd", "OnAfterFileAdd", "OnBeforeFileUpdate", "OnAfterFileUpdate"),
        "form" to setOf("OnBeforeFormAdd", "OnAfterFormAdd", "OnBeforeFormResultAdd", "OnAfterFormResultAdd"),
        "subscribe" to setOf("OnBeforeSubscribeAdd", "OnAfterSubscribeAdd", "OnBeforeSubscribeUpdate", "OnAfterSubscribeUpdate"),
        "ui" to setOf("OnUiFormShow", "OnUiFilterApply"),
        "pull" to setOf("OnPullEvent", "OnGetDependentModule"),
    )

    fun forModule(moduleId: String?): Set<String> {
        if (moduleId != null && byModule.containsKey(moduleId)) return byModule.getValue(moduleId)
        return byModule.values.flatten().toSortedSet()
    }

    fun modules(): Set<String> = byModule.keys
}
