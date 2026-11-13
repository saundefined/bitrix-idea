package com.github.saundefined.bitrix_idea.templates.component

import java.util.Locale

/**
 * Заготовка `class.php` компонента.
 */
object ComponentClassGenerator {
    fun generate(
        className: String,
        cache: Boolean = true,
        ajax: Boolean = false,
        epilog: Boolean = false,
    ): String {
        return buildString {
            appendLine("<?php")
            appendLine()
            appendLine("if (!defined('B_PROLOG_INCLUDED') || B_PROLOG_INCLUDED !== true) {")
            appendLine("    die();")
            appendLine("}")
            appendLine()
            appendLine("class $className extends \\CBitrixComponent")
            appendLine("{")
            appendLine("    public function onPrepareComponentParams(\$arParams): array")
            appendLine("    {")
            appendLine("        return \$arParams;")
            appendLine("    }")
            if (ajax) {
                appendLine()
                appendLine("    public function configureActions(): array")
                appendLine("    {")
                appendLine("        return [];")
                appendLine("    }")
            }
            appendLine()
            appendLine("    public function executeComponent(): void")
            appendLine("    {")
            if (cache) {
                appendLine("        if (\$this->startResultCache()) {")
                appendLine("            \$this->arResult = [];")
                appendLine("            \$this->includeComponentTemplate();")
                appendLine("            \$this->endResultCache();")
                appendLine("        }")
            } else {
                appendLine("        \$this->arResult = [];")
                appendLine("        \$this->includeComponentTemplate();")
            }
            appendLine("    }")
            if (epilog) {
                appendLine()
                appendLine("    public function executeComponentEpilog(): void")
                appendLine("    {")
                appendLine("    }")
            }
            appendLine("}")
        }
    }

    fun classNameFor(componentName: String): String {
        val parts = componentName.split('.', '_', '-')
            .filter { it.isNotEmpty() }
            .map { it.replaceFirstChar { c -> c.titlecase(Locale.ROOT) } }
        val name = parts.joinToString("")
        return if (name.endsWith("Component")) name else name + "Component"
    }
}
