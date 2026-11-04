package com.github.saundefined.bitrix_idea.templates.complex

import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.util.Locale

data class ComplexComponentPage(
    val code: String,
    val url: String = "",
    val variables: List<String> = emptyList(),
)

data class ComplexComponentSpec(
    val vendor: String,
    val name: String,
    val title: String,
    val description: String,
    val pages: List<ComplexComponentPage>,
    val languages: List<String>,
    val fallbackTemplate: Boolean = false,
    val style: Boolean = true,
    val script: Boolean = true,
    val resultModifier: Boolean = false,
    val componentEpilog: Boolean = false,
    val ajax: Boolean = false,
)

object ComplexComponentWriter {
    fun write(baseDirectory: VirtualFile, spec: ComplexComponentSpec): VirtualFile {
        val vendorDirectory = if (baseDirectory.name == spec.vendor) {
            baseDirectory
        } else {
            baseDirectory.findChild(spec.vendor) ?: baseDirectory.createChildDirectory(this, spec.vendor)
        }
        val root = vendorDirectory.createChildDirectory(this, spec.name)
        val upper = "${spec.vendor}_${spec.name}"
            .uppercase(Locale.ROOT)
            .replace(Regex("[^A-Z0-9]+"), "_")
            .trim('_')
        val camel = "${spec.vendor}_${spec.name}".split('_', '.', '-')
            .filter { it.isNotEmpty() }
            .joinToString("") { it.replaceFirstChar { c -> c.titlecase(Locale.ROOT) } }
        val css = "${spec.vendor}-${spec.name}".lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')

        write(root, ".description.php", description(spec, upper))
        write(root, ".parameters.php", parameters(spec, upper))
        write(root, "class.php", componentClass(spec, camel, upper))

        val template = root.createChildDirectory(this, "templates").createChildDirectory(this, ".default")
        spec.pages.forEach { page ->
            write(template, "${page.code}.php", templatePage(spec, upper, css, page.code))
        }
        if (spec.fallbackTemplate) {
            write(template, "template.php", templatePage(spec, upper, css, "template"))
        }
        if (spec.style) write(template, "style.css", ".$css {\n}\n")
        if (spec.script) write(template, "script.js", "BX.ready(() => {\n    // Логика компонента.\n});\n")
        if (spec.resultModifier) write(template, "result_modifier.php", resultModifier())
        if (spec.componentEpilog) write(template, "component_epilog.php", epilog())

        spec.languages.forEach { language ->
            val lang = root.createChildDirectory(this, "lang").createChildDirectory(this, language)
            write(lang, ".description.php", langDescription(spec, upper))
            write(lang, ".parameters.php", langParameters(spec, upper))
            spec.pages.forEach { page ->
                write(lang, "${page.code}.php", "<?php\n\n\$MESS['${upper}_${page.code.uppercase()}_TITLE'] = '${page.code}';\n")
            }
            if (spec.fallbackTemplate) {
                write(lang, "template.php", "<?php\n\n\$MESS['${upper}_TEMPLATE_TITLE'] = '${spec.title}';\n")
            }
            if (spec.componentEpilog) {
                val epilogLang = template.createChildDirectory(this, "lang").createChildDirectory(this, language)
                if (epilogLang.findChild("component_epilog.php") == null) {
                    write(epilogLang, "component_epilog.php", "<?php\n\n// Фразы для component_epilog.php.\n")
                }
            }
        }
        return root
    }

    private fun description(spec: ComplexComponentSpec, upper: String): String = """
        |<?php
        |
        |use Bitrix\Main\Localization\Loc;
        |
        |if (!defined('B_PROLOG_INCLUDED') || B_PROLOG_INCLUDED !== true) {
        |    die();
        |}
        |
        |Loc::loadMessages(__FILE__);
        |
        |${'$'}arComponentDescription = [
        |    'NAME' => Loc::getMessage('${upper}_NAME'),
        |    'DESCRIPTION' => Loc::getMessage('${upper}_DESCRIPTION'),
        |    'PATH' => [
        |        'ID' => '${spec.vendor}',
        |        'NAME' => '${spec.vendor}',
        |    ],
        |    'CACHE_PATH' => 'Y',
        |    'COMPLEX' => 'Y',
        |];
        |""".trimMargin()

    private fun parameters(spec: ComplexComponentSpec, upper: String): String {
        val sef = spec.pages.joinToString(",\n") { page ->
            val vars = page.variables.joinToString(", ") { "'$it'" }
            "        '${page.code}' => [\n            'NAME' => Loc::getMessage('${upper}_SEF_${page.code.uppercase()}'),\n            'DEFAULT' => '${page.url}',\n            'VARIABLES' => [$vars],\n        ]"
        }
        return """
            |<?php
            |
            |use Bitrix\Main\Localization\Loc;
            |
            |if (!defined('B_PROLOG_INCLUDED') || B_PROLOG_INCLUDED !== true) {
            |    die();
            |}
            |
            |Loc::loadMessages(__FILE__);
            |
            |${'$'}arComponentParameters = [
            |    'GROUPS' => [
            |        'SEF' => ['NAME' => Loc::getMessage('${upper}_SEF_GROUP')],
            |    ],
            |    'PARAMETERS' => [
            |        'SEF_MODE' => [
            |$sef
            |        ],
            |        'SEF_FOLDER' => [
            |            'PARENT' => 'SEF',
            |            'NAME' => Loc::getMessage('${upper}_SEF_FOLDER'),
            |            'TYPE' => 'STRING',
            |            'DEFAULT' => '/${spec.name}/',
            |        ],
            |        'CACHE_TIME' => [],
            |    ],
            |];
            |""".trimMargin()
    }

    private fun componentClass(spec: ComplexComponentSpec, camel: String, upper: String): String {
        val ajaxInterface = if (spec.ajax) {
            " implements \\Bitrix\\Main\\Engine\\Contract\\Controllerable"
        } else {
            ""
        }
        val ajaxMethods = if (spec.ajax) {
            """
            |
            |    public function configureActions(): array
            |    {
            |        return ['list' => []];
            |    }
            |
            |    public function listAction(): array
            |    {
            |        return [];
            |    }
            |
            |    protected function listKeysSignedParameters(): array
            |    {
            |        return ['SEF_FOLDER'];
            |    }
            |""".trimMargin()
        } else {
            ""
        }
        val pages = spec.pages.joinToString(", ") { "'${it.code}' => '${it.url}'" }
        val pagesArray = spec.pages.joinToString(",\n") { "                '${it.code}' => '${it.url}'" }

        return """
            |<?php
            |
            |if (!defined('B_PROLOG_INCLUDED') || B_PROLOG_INCLUDED !== true) {
            |    die();
            |}
            |
            |final class ${camel}Component extends \CBitrixComponent$ajaxInterface
            |{
            |    public function onPrepareComponentParams(${'$'}arParams): array
            |    {
            |        ${'$'}arParams['SEF_FOLDER'] = rtrim((string)(${'$'}arParams['SEF_FOLDER'] ?? ''), '/').'/';
            |        ${'$'}arParams['CACHE_TIME'] = (int)(${'$'}arParams['CACHE_TIME'] ?? 3600);
            |
            |        return ${'$'}arParams;
            |    }
            |
            |    public function executeComponent(): void
            |    {
            |        ${'$'}templates = \CComponentEngine::makeComponentUrlTemplates(
            |            [
            |$pagesArray
            |            ],
            |            ${'$'}this->arParams['SEF_URL_TEMPLATES'] ?? [],
            |        );
            |
            |        ${'$'}variables = [];
            |        ${'$'}page = (new \CComponentEngine(${'$'}this))
            |            ->guessComponentPath(${'$'}this->arParams['SEF_FOLDER'], ${'$'}templates, ${'$'}variables) ?: '${spec.pages.firstOrNull()?.code ?: "list"}';
            |
            |        ${'$'}this->arResult = [
            |            'FOLDER' => ${'$'}this->arParams['SEF_FOLDER'],
            |            'URL_TEMPLATES' => ${'$'}templates,
            |            'VARIABLES' => ${'$'}variables,
            |        ];
            |
            |        if (${'$'}this->startResultCache(false, [${'$'}page, ${'$'}variables])) {
            |            ${'$'}this->initResult(${'$'}page);
            |            ${'$'}this->includeComponentTemplate(${'$'}page);
            |        }
            |    }
            |
            |    private function initResult(string ${'$'}page): void
            |    {
            |        // Заготовка: заполните arResult данными для страницы.
            |    }
            |$ajaxMethods
            |}
            |""".trimMargin()
    }

    private fun templatePage(spec: ComplexComponentSpec, upper: String, css: String, page: String): String = """
        |<?php
        |
        |use Bitrix\Main\Localization\Loc;
        |
        |if (!defined('B_PROLOG_INCLUDED') || B_PROLOG_INCLUDED !== true) {
        |    die();
        |}
        |
        |Loc::loadMessages(__FILE__);
        |
        |/**
        | * @var array ${'$'}arParams
        | * @var array ${'$'}arResult
        | */
        |?>
        |<div class="$css">
        |    <h2><?= htmlspecialcharsbx(Loc::getMessage('${upper}_${page.uppercase()}_TITLE')) ?></h2>
        |</div>
        |""".trimMargin()

    private fun resultModifier(): String = "<?php\n\nif (!defined('B_PROLOG_INCLUDED') || B_PROLOG_INCLUDED !== true) {\n    die();\n}\n\n// Подготовьте \$arResult перед выводом шаблона.\n"

    private fun epilog(): String = "<?php\n\nuse Bitrix\\Main\\Localization\\Loc;\n\nif (!defined('B_PROLOG_INCLUDED') || B_PROLOG_INCLUDED !== true) {\n    die();\n}\n\nLoc::loadMessages(__FILE__);\n\n// Код после шаблона, в том числе при загрузке из кеша.\n"

    private fun langDescription(spec: ComplexComponentSpec, upper: String): String = "<?php\n\n\$MESS['${upper}_NAME'] = '${spec.title}';\n\$MESS['${upper}_DESCRIPTION'] = '${spec.description}';\n"

    private fun langParameters(spec: ComplexComponentSpec, upper: String): String {
        val pages = spec.pages.joinToString("") { page ->
            "\$MESS['${upper}_SEF_${page.code.uppercase()}'] = '${page.code}';\n"
        }
        return "<?php\n\n\$MESS['${upper}_SEF_GROUP'] = 'ЧПУ';\n\$MESS['${upper}_SEF_FOLDER'] = 'Каталог ЧПУ';\n$pages"
    }

    private fun write(directory: VirtualFile, name: String, content: String) {
        val file = directory.findChild(name) ?: directory.createChildData(this, name)
        VfsUtil.saveText(file, content)
    }
}
