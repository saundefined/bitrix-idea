package com.github.saundefined.bitrix_idea.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "com.github.saundefined.bitrix_idea.settings.BitrixProjectSettings",
    storages = [Storage("bitrixIdea.xml")],
)
class BitrixProjectSettings : PersistentStateComponent<BitrixProjectSettings> {
    var bitrixFrameworkEnabled: Boolean = false
    var bitrixDetectionNotificationShown: Boolean = false

    /**
     * Подключать встроенную библиотеку `bitrix-idea-library`.
     * Оставлено для совместимости со старыми настройками.
     */
    var useBundledLibrary: Boolean = true

    /**
     * Источник ядра: встроенная библиотека или `bitrix` из проекта.
     */
    var coreSource: BitrixCoreSource = BitrixCoreSource.BUNDLED

    /**
     * Показывать дерево модулей и компонентов в окне Bitrix.
     */
    var bitrixToolWindowEnabled: Boolean = true

    /**
     * Показывать текст lang-фразы вместо Loc::getMessage(...).
     */
    var langFoldingEnabled: Boolean = true

    /**
     * Предпочитаемый язык lang-подсказок. Пусто — первый найденный.
     */
    var langHintLanguage: String = "ru"

    /**
     * Каталог, содержащий `bitrix` и `local`, относительно корня проекта.
     * Пустое значение означает корень проекта. Примеры: `www`, `public`, `../site`.
     */
    var bitrixRootPath: String = ""

    /**
     * Помечать модули проекта как source roots и исключать отключённые из индексации.
     */
    var autoConfigureSourceRoots: Boolean = true

    /**
     * ID модулей, которые не нужно индексировать. Пустой список — индексируются все.
     */
    var disabledModules: List<String> = emptyList()

    /**
     * ID компонентов, которые не нужно индексировать (`vendor:name`).
     */
    var excludedComponentIds: List<String> = emptyList()

    /**
     * Шаблоны сайтов, которые не нужно индексировать.
     */
    var excludedSiteTemplates: List<String> = emptyList()

    /**
     * Увеличивается при смене корневого каталога, чтобы сбросить кеш индексов проекта.
     */
    var settingsRevision: Long = 0

    override fun getState(): BitrixProjectSettings = this

    override fun loadState(state: BitrixProjectSettings) {
        XmlSerializerUtil.copyBean(state, this)
        // Совместимость со старой настройкой useBundledLibrary.
        if (!useBundledLibrary && coreSource == BitrixCoreSource.BUNDLED) {
            coreSource = BitrixCoreSource.PROJECT
        }
    }

    companion object {
        fun getInstance(project: Project): BitrixProjectSettings = project.service()
    }
}

enum class BitrixCoreSource(val displayName: String) {
    BUNDLED("Встроенная библиотека"),
    PROJECT("bitrix из проекта"),
}
