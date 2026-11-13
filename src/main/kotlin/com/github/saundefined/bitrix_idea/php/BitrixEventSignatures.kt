package com.github.saundefined.bitrix_idea.php

/**
 * Подсказки по типовым сигнатурам обработчиков событий.
 */
object BitrixEventSignatures {
    private val signatures = mapOf(
        "OnBeforeProlog" to "public static function onBeforeProlog(): void",
        "OnProlog" to "public static function onProlog(): void",
        "OnEpilog" to "public static function onEpilog(): void",
        "OnEndBufferContent" to "public static function onEndBufferContent(string &\$content): void",
        "OnBuildGlobalMenu" to "public static function onBuildGlobalMenu(array &\$globalMenu, array &\$moduleMenu): void",
        "OnBeforeUserAdd" to "public static function onBeforeUserAdd(array &\$fields): bool",
        "OnAfterUserAdd" to "public static function onAfterUserAdd(array \$fields): void",
        "OnBeforeUserUpdate" to "public static function onBeforeUserUpdate(array &\$fields): bool",
        "OnAfterUserUpdate" to "public static function onAfterUserUpdate(array \$fields): void",
        "OnBeforeUserDelete" to "public static function onBeforeUserDelete(int \$userId): bool",
        "OnAfterUserDelete" to "public static function onAfterUserDelete(int \$userId): void",
        "OnBeforeIBlockElementAdd" to "public static function onBeforeIBlockElementAdd(array &\$fields): bool",
        "OnAfterIBlockElementAdd" to "public static function onAfterIBlockElementAdd(array \$fields): void",
        "OnBeforeIBlockElementUpdate" to "public static function onBeforeIBlockElementUpdate(array &\$fields): bool",
        "OnAfterIBlockElementUpdate" to "public static function onAfterIBlockElementUpdate(array \$fields): void",
        "OnBeforeIBlockElementDelete" to "public static function onBeforeIBlockElementDelete(int \$elementId): bool",
        "OnAfterIBlockElementDelete" to "public static function onAfterIBlockElementDelete(array \$fields): void",
        "OnBeforeEventSend" to "public static function onBeforeEventSend(array &\$fields, array &\$template): void",
        "OnBeforeMailSend" to "public static function onBeforeMailSend(array &\$fields, array &\$mail): bool",
        "OnSaleOrderSaved" to "public static function onSaleOrderSaved(\\Bitrix\\Main\\Event \$event): void",
        "OnSaleOrderBeforeSaved" to "public static function onSaleOrderBeforeSaved(\\Bitrix\\Main\\Event \$event): \\Bitrix\\Main\\EventResult",
        "OnSaleOrderDeleted" to "public static function onSaleOrderDeleted(\\Bitrix\\Sale\\Internals\\Order \$order): void",
        "OnSaleOrderPaid" to "public static function onSaleOrderPaid(\\Bitrix\\Sale\\Internals\\Order \$order): void",
        "OnGetOptimalPrice" to "public static function onGetOptimalPrice(int \$productId, int \$quantity, array \$userGroups, string \$renewal, array \$prices): array",
    )

    fun forEvent(eventName: String): String? = signatures[eventName]

    fun generic(eventName: String): String {
        return "public static function handle${eventName.removePrefix("On")}(): void"
    }
}
