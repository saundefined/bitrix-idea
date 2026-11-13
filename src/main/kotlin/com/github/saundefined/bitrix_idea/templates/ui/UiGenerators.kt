package com.github.saundefined.bitrix_idea.templates.ui

import java.util.Locale

/**
 * Генерация конфигураций Bitrix UI и простых admin-страниц.
 */
object UiGenerators {
    fun generateGridConfig(namespace: String, entity: String, tableClass: String, fields: List<String>): String {
        return buildString {
            appendLine("<?php")
            appendLine()
            appendLine("namespace $namespace\\Ui;")
            appendLine()
            appendLine("use $namespace\\$tableClass;")
            appendLine()
            appendLine("class ${entity}Grid")
            appendLine("{")
            appendLine("    public static function getColumns(): array")
            appendLine("    {")
            appendLine("        return [")
            fields.forEach { field ->
                appendLine("            [")
                appendLine("                'id' => '$field',")
                appendLine("                'name' => '$field',")
                appendLine("                'sort' => '$field',")
                appendLine("                'default' => ${if (field == fields.first()) "true" else "false"},")
                appendLine("            ],")
            }
            appendLine("        ];")
            appendLine("    }")
            appendLine()
            appendLine("    public static function getRows(int \$limit = 50): array")
            appendLine("    {")
            appendLine("        return $tableClass::getList([")
            appendLine("            'select' => ['${fields.joinToString("', '")}'],")
            appendLine("            'limit' => \$limit,")
            appendLine("        ])->fetchAll();")
            appendLine("    }")
            appendLine("}")
        }
    }

    fun generateFilterConfig(namespace: String, entity: String, fields: List<String>): String {
        return buildString {
            appendLine("<?php")
            appendLine()
            appendLine("namespace $namespace\\Ui;")
            appendLine()
            appendLine("class ${entity}Filter")
            appendLine("{")
            appendLine("    public static function getFields(): array")
            appendLine("    {")
            appendLine("        return [")
            fields.forEach { field ->
                appendLine("            '$field' => [")
                appendLine("                'id' => '$field',")
                appendLine("                'name' => '$field',")
                appendLine("                'type' => 'string',")
                appendLine("            ],")
            }
            appendLine("        ];")
            appendLine("    }")
            appendLine("}")
        }
    }

    fun generateFormConfig(namespace: String, entity: String, fields: List<String>): String {
        return buildString {
            appendLine("<?php")
            appendLine()
            appendLine("namespace $namespace\\Ui;")
            appendLine()
            appendLine("class ${entity}Form")
            appendLine("{")
            appendLine("    public static function getFields(): array")
            appendLine("    {")
            appendLine("        return [")
            fields.forEach { field ->
                appendLine("            '$field' => [")
                appendLine("                'label' => '$field',")
                appendLine("                'type' => 'text',")
                appendLine("            ],")
            }
            appendLine("        ];")
            appendLine("    }")
            appendLine("}")
        }
    }

    fun generateListPage(
        moduleId: String,
        namespace: String,
        entity: String,
        title: String,
    ): String {
        val listId = "${moduleId.replace(Regex("[^a-z0-9]+"), "_")}_${entity.lowercase(Locale.ROOT)}"
        return buildString {
            appendLine("<?php")
            appendLine()
            appendLine("use Bitrix\\Main\\Loader;")
            appendLine("use Bitrix\\Main\\Localization\\Loc;")
            appendLine("use $namespace\\Ui\\${entity}Grid;")
            appendLine("use $namespace\\Ui\\${entity}Filter;")
            appendLine()
            appendLine("define('ADMIN_MODULE_NAME', '$moduleId');")
            appendLine("require_once \$_SERVER['DOCUMENT_ROOT'].'/bitrix/modules/main/include/prolog_admin_before.php';")
            appendLine("Loc::loadMessages(__FILE__);")
            appendLine("if (!Loader::includeModule(ADMIN_MODULE_NAME)) {")
            appendLine("    \$APPLICATION->AuthForm(Loc::getMessage('ACCESS_DENIED'));")
            appendLine("}")
            appendLine("\$APPLICATION->SetTitle('${escape(title)}');")
            appendLine("require_once \$_SERVER['DOCUMENT_ROOT'].'/bitrix/modules/main/include/prolog_admin_after.php';")
            appendLine()
            appendLine("\$headers = [];")
            appendLine("foreach (${entity}Grid::getColumns() as \$column) {")
            appendLine("    \$headers[] = \$column;")
            appendLine("}")
            appendLine("\$list = new CAdminUiList('$listId', \$headers);")
            appendLine("\$list->AddHeaders(\$headers);")
            appendLine("\$list->AddFilter(${entity}Filter::getFields(), []);")
            appendLine("foreach (${entity}Grid::getRows() as \$row) {")
            appendLine("    \$list->AddRow(\$row['ID'], \$row);")
            appendLine("}")
            appendLine("\$list->DisplayList();")
            appendLine("require_once \$_SERVER['DOCUMENT_ROOT'].'/bitrix/modules/main/include/epilog_admin.php';")
        }
    }

    fun generateEditPage(moduleId: String, namespace: String, entity: String, title: String): String {
        return buildString {
            appendLine("<?php")
            appendLine()
            appendLine("use Bitrix\\Main\\Loader;")
            appendLine("use Bitrix\\Main\\Localization\\Loc;")
            appendLine("use $namespace\\Ui\\${entity}Form;")
            appendLine()
            appendLine("define('ADMIN_MODULE_NAME', '$moduleId');")
            appendLine("require_once \$_SERVER['DOCUMENT_ROOT'].'/bitrix/modules/main/include/prolog_admin_before.php';")
            appendLine("Loc::loadMessages(__FILE__);")
            appendLine("if (!Loader::includeModule(ADMIN_MODULE_NAME)) {")
            appendLine("    \$APPLICATION->AuthForm(Loc::getMessage('ACCESS_DENIED'));")
            appendLine("}")
            appendLine("\$APPLICATION->SetTitle('${escape(title)}');")
            appendLine("require_once \$_SERVER['DOCUMENT_ROOT'].'/bitrix/modules/main/include/prolog_admin_after.php';")
            appendLine()
            appendLine("\$form = new CAdminForm('${entity.lowercase(Locale.ROOT)}_edit', [")
            appendLine("    ['DIV' => 'edit1', 'TAB' => '${escape(title)}', 'TITLE' => '${escape(title)}'],")
            appendLine("]);")
            appendLine("\$form->Begin(['FORM_ACTION' => \$APPLICATION->GetCurPage()]);")
            appendLine("\$form->BeginNextTab();")
            appendLine("foreach (${entity}Form::getFields() as \$name => \$field) {")
            appendLine("    \$form->AddInput(\$name, \$field['label'], [], []);")
            appendLine("}")
            appendLine("\$form->Buttons(['btnSave' => true, 'btnApply' => true, 'btnCancel' => true]);")
            appendLine("\$form->End();")
            appendLine("require_once \$_SERVER['DOCUMENT_ROOT'].'/bitrix/modules/main/include/epilog_admin.php';")
        }
    }

    private fun escape(value: String): String = value.replace("\\", "\\\\").replace("'", "\\'")
}
