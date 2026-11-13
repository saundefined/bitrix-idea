package com.github.saundefined.bitrix_idea.templates.module

import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleGeneratorsTest {
    @Test
    fun generatesOptionsPage() {
        val options = listOf(
            ModuleOptionSpec("enabled", "Включено", ModuleOptionType.CHECKBOX, "Y"),
            ModuleOptionSpec("api_key", "API-ключ", ModuleOptionType.TEXT),
        )

        val page = OptionsPageGenerator.generate("vendor.module", options)
        val lang = OptionsPageGenerator.generateLang(options)

        assertTrue(page.contains("\$module_id = 'vendor.module';"))
        assertTrue(page.contains("'enabled' => ["))
        assertTrue(page.contains("'TYPE' => 'checkbox'"))
        assertTrue(page.contains("__AdmSettingsDrawList"))
        assertTrue(lang.contains("\$MESS['OPTION_API_KEY'] = 'API-ключ';"))
    }

    @Test
    fun generatesAdminPageAndMenu() {
        val page = AdminPageGenerator.generatePage("vendor.module", "settings.php", "Настройки")
        val menu = AdminPageGenerator.generateMenu(
            moduleId = "vendor.module",
            pageFile = "settings.php",
            title = "Настройки",
            parentMenu = "global_menu_content",
            sort = 100,
            section = "menu_vendor_module",
        )
        val pageLang = AdminPageGenerator.generatePageLang("Настройки", AdminPageGenerator.pageTitleKey("settings.php"))

        assertTrue(page.contains("ADMIN_MODULE_NAME"))
        assertTrue(page.contains("Loc::getMessage('PAGE_SETTINGS')"))
        assertTrue(menu.contains("'url' => 'settings.php?lang='.LANGUAGE_ID"))
        assertTrue(pageLang.contains("\$MESS['PAGE_SETTINGS'] = 'Настройки';"))
    }
}
