package com.github.saundefined.bitrix_idea.templates.module

import java.util.Locale

enum class ModuleOptionType(val displayName: String, val bitrixType: String) {
    TEXT("Строка", "text"),
    CHECKBOX("Флажок", "checkbox"),
    TEXTAREA("Текст", "textarea"),
    PASSWORD("Пароль", "password"),
    SELECT("Список", "select"),
}

data class ModuleOptionSpec(
    val key: String,
    val name: String,
    val type: ModuleOptionType = ModuleOptionType.TEXT,
    val defaultValue: String = "",
)

/**
 * Генерация `options.php` модуля.
 */
object OptionsPageGenerator {
    fun generate(moduleId: String, options: List<ModuleOptionSpec>): String {
        val entries = options.joinToString(",\n") { option ->
            buildString {
                append("    '").append(escape(option.key)).append("' => [\n")
                append("        'NAME' => Loc::getMessage('").append(langKey(option.key)).append("'),\n")
                append("        'TYPE' => '").append(option.type.bitrixType).append("',\n")
                if (option.defaultValue.isNotBlank()) {
                    append("        'DEFAULT' => '").append(escape(option.defaultValue)).append("',\n")
                }
                append("    ]")
            }
        }
        return buildString {
            appendLine("<?php")
            appendLine()
            appendLine("use Bitrix\\Main\\Config\\Option;")
            appendLine("use Bitrix\\Main\\Localization\\Loc;")
            appendLine()
            appendLine("Loc::loadMessages(__FILE__);")
            appendLine()
            append("\$module_id = '").append(escape(moduleId)).appendLine("';")
            appendLine("\$options = [")
            if (entries.isNotBlank()) appendLine(entries)
            appendLine("];")
            appendLine()
            appendLine("if (\$REQUEST_METHOD === 'POST' && check_bitrix_sessid()) {")
            appendLine("    __AdmSettingsSaveOptions(\$module_id, \$options);")
            appendLine("}")
            appendLine()
            appendLine("\$tabs = [")
            appendLine("    ['DIV' => 'edit1', 'TAB' => Loc::getMessage('OPTIONS_TAB'), 'TITLE' => Loc::getMessage('OPTIONS_TAB_TITLE')],")
            appendLine("];")
            appendLine("\$tabControl = new CAdminTabControl('tabControl', \$tabs);")
            appendLine()
            appendLine("\$tabControl->Begin();")
            appendLine("\$tabControl->BeginNextTab();")
            appendLine("__AdmSettingsDrawList(\$module_id, \$options);")
            appendLine("\$tabControl->Buttons();")
            appendLine("?>")
            appendLine("<input type=\"submit\" name=\"Update\" value=\"<?= Loc::getMessage('OPTIONS_SAVE') ?>\">")
            appendLine("<?= bitrix_sessid_post() ?>")
            appendLine("<?php")
            appendLine("\$tabControl->End();")
        }
    }

    fun generateLang(options: List<ModuleOptionSpec>): String {
        return buildString {
            appendLine("<?php")
            appendLine()
            appendLine("\$MESS['OPTIONS_TAB'] = 'Настройки';")
            appendLine("\$MESS['OPTIONS_TAB_TITLE'] = 'Настройки модуля';")
            appendLine("\$MESS['OPTIONS_SAVE'] = 'Сохранить';")
            options.forEach { option ->
                appendLine("\$MESS['${langKey(option.key)}'] = '${escape(option.name)}';")
            }
        }
    }

    fun langKey(key: String): String = "OPTION_${key.uppercase(Locale.ROOT).replace(Regex("[^A-Z0-9_]+"), "_")}"

    private fun escape(value: String): String = value.replace("\\", "\\\\").replace("'", "\\'")
}
