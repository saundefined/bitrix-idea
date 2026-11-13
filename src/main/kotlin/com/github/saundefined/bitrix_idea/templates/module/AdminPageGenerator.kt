package com.github.saundefined.bitrix_idea.templates.module

import java.util.Locale

/**
 * Генерация admin-страницы и `admin/menu.php`.
 */
object AdminPageGenerator {
    fun generatePage(moduleId: String, pageFile: String, title: String): String {
        val titleKey = pageTitleKey(pageFile)
        return buildString {
            appendLine("<?php")
            appendLine()
            appendLine("use Bitrix\\Main\\Loader;")
            appendLine("use Bitrix\\Main\\Localization\\Loc;")
            appendLine()
            appendLine("define('ADMIN_MODULE_NAME', '$moduleId');")
            appendLine("require_once \$_SERVER['DOCUMENT_ROOT'].'/bitrix/modules/main/include/prolog_admin_before.php';")
            appendLine()
            appendLine("Loc::loadMessages(__FILE__);")
            appendLine()
            appendLine("if (!Loader::includeModule(ADMIN_MODULE_NAME)) {")
            appendLine("    \$APPLICATION->AuthForm(Loc::getMessage('ACCESS_DENIED'));")
            appendLine("}")
            appendLine("if (\$APPLICATION->GetGroupRight(ADMIN_MODULE_NAME) < 'R') {")
            appendLine("    \$APPLICATION->AuthForm(Loc::getMessage('ACCESS_DENIED'));")
            appendLine("}")
            appendLine()
            appendLine("\$APPLICATION->SetTitle(Loc::getMessage('$titleKey'));")
            appendLine("require_once \$_SERVER['DOCUMENT_ROOT'].'/bitrix/modules/main/include/prolog_admin_after.php';")
            appendLine("?>")
            appendLine("<div class=\"adm-info-message-wrap\">")
            appendLine("    <?= Loc::getMessage('$titleKey') ?>")
            appendLine("</div>")
            appendLine("<?php")
            appendLine("require_once \$_SERVER['DOCUMENT_ROOT'].'/bitrix/modules/main/include/epilog_admin.php';")
        }
    }

    fun generateMenu(
        moduleId: String,
        pageFile: String,
        title: String,
        parentMenu: String,
        sort: Int,
        section: String,
    ): String {
        return buildString {
            appendLine("<?php")
            appendLine()
            appendLine("use Bitrix\\Main\\Localization\\Loc;")
            appendLine()
            appendLine("Loc::loadMessages(__FILE__);")
            appendLine()
            appendLine("\$moduleId = '$moduleId';")
            appendLine("\$aMenu = [];")
            appendLine()
            appendLine("if (\$APPLICATION->GetGroupRight(\$moduleId) >= 'R') {")
            appendLine("    \$aMenu[] = [")
            append("        'parent_menu' => '").append(escape(parentMenu)).appendLine("',")
            append("        'section' => '").append(escape(section)).appendLine("',")
            appendLine("        'sort' => $sort,")
            appendLine("        'text' => Loc::getMessage('MENU_TEXT'),")
            appendLine("        'title' => Loc::getMessage('MENU_TITLE'),")
            append("        'url' => '").append(escape(pageFile)).appendLine("?lang='.LANGUAGE_ID,")
            append("        'items_id' => 'menu_").append(section.replace(Regex("[^a-zA-Z0-9_]+"), "_")).appendLine("',")
            appendLine("    ];")
            appendLine("}")
            appendLine()
            appendLine("return \$aMenu;")
        }
    }

    fun generatePageLang(title: String, titleKey: String = "PAGE_TITLE"): String {
        return buildString {
            appendLine("<?php")
            appendLine()
            appendLine("\$MESS['ACCESS_DENIED'] = 'Доступ запрещён';")
            appendLine("\$MESS['$titleKey'] = '${escape(title)}';")
        }
    }

    fun generateMenuLang(title: String): String {
        return buildString {
            appendLine("<?php")
            appendLine()
            appendLine("\$MESS['MENU_TEXT'] = '${escape(title)}';")
            appendLine("\$MESS['MENU_TITLE'] = '${escape(title)}';")
        }
    }

    fun pageTitleKey(fileName: String): String {
        return "PAGE_${fileName.removeSuffix(".php").uppercase(Locale.ROOT).replace(Regex("[^A-Z0-9_]+"), "_")}"
    }

    private fun escape(value: String): String = value.replace("\\", "\\\\").replace("'", "\\'")
}
