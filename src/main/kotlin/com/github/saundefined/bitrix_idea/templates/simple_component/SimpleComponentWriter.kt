package com.github.saundefined.bitrix_idea.templates.simple_component

import com.github.saundefined.bitrix_idea.templates.BitrixTemplateNames
import com.github.saundefined.bitrix_idea.templates.BitrixTemplateWriter
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.psi.PsiDirectory
import java.util.Locale
import java.util.Properties

data class SimpleComponentSpec(
    val vendor: String,
    val name: String,
    val displayName: String,
    val description: String,
    val vendorName: String,
    val languages: List<String>,
    val resultModifier: Boolean = false,
    val componentEpilog: Boolean = false,
    val complex: Boolean = false,
)

object SimpleComponentWriter {
    fun write(
        templateManager: FileTemplateManager,
        baseDirectory: PsiDirectory,
        spec: SimpleComponentSpec,
        properties: Properties,
    ): PsiDirectory {
        val vendorDirectory = if (baseDirectory.name == spec.vendor) {
            baseDirectory
        } else {
            baseDirectory.findSubdirectory(spec.vendor) ?: baseDirectory.createSubdirectory(spec.vendor)
        }
        val componentDirectory = vendorDirectory.createSubdirectory(spec.name)

        val rawName = "${spec.vendor}_${spec.name}"
        val uppercaseName = rawName.uppercase(Locale.ROOT).replace(Regex("[^A-Z0-9]+"), "_").trim('_')
        val camelName = rawName.split('.', '_', '-')
            .filter { it.isNotEmpty() }
            .joinToString("") { it.replaceFirstChar { c -> c.titlecase(Locale.ROOT) } }
        val cssClass = "${spec.vendor}-${spec.name}"
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')

        properties.setProperty("COMPONENT_VENDOR", spec.vendor)
        properties.setProperty("COMPONENT_VENDOR_NAME", escapePhp(spec.vendorName))
        properties.setProperty("COMPONENT_NAME", escapePhp(spec.displayName))
        properties.setProperty("COMPONENT_DESCRIPTION", escapePhp(spec.description))
        properties.setProperty("COMPONENT_CODE", spec.name)
        properties.setProperty("COMPONENT_DIRECTORY", "${spec.vendor}/${spec.name}")
        properties.setProperty("COMPONENT_CSS_CLASS", cssClass)
        properties.setProperty("UPPERCASE_COMPONENT_NAME", uppercaseName)
        properties.setProperty("CAMEL_COMPONENT_NAME", camelName)

        BitrixTemplateWriter.createFiles(
            templateManager,
            componentDirectory,
            BitrixTemplateNames.SIMPLE_COMPONENT,
            listOf(".description.php", ".parameters.php", "class.php"),
            properties,
        )

        if (spec.complex) {
            com.intellij.openapi.vfs.VfsUtil.saveText(
                componentDirectory.virtualFile.createChildData(this, "component.php"),
                "<?php\n\nif (!defined('B_PROLOG_INCLUDED') || B_PROLOG_INCLUDED !== true) {\n    die();\n}\n\nrequire_once __DIR__.'/class.php';\n",
            )
        }

        val defaultTemplateDirectory = componentDirectory
            .createSubdirectory("templates")
            .createSubdirectory(".default")
        BitrixTemplateWriter.createFile(
            templateManager,
            defaultTemplateDirectory,
            BitrixTemplateNames.SIMPLE_COMPONENT_TEMPLATE,
            "template.php",
            properties,
        )
        BitrixTemplateWriter.createFile(
            templateManager,
            defaultTemplateDirectory,
            BitrixTemplateNames.SIMPLE_COMPONENT_TEMPLATE_SCRIPT,
            "script.js",
            properties,
        )
        BitrixTemplateWriter.createFile(
            templateManager,
            defaultTemplateDirectory,
            BitrixTemplateNames.SIMPLE_COMPONENT_TEMPLATE_STYLE,
            "style.css",
            properties,
        )
        if (spec.resultModifier) {
            BitrixTemplateWriter.createFile(
                templateManager,
                defaultTemplateDirectory,
                BitrixTemplateNames.SIMPLE_COMPONENT_TEMPLATE_RESULT_MODIFIER,
                "result_modifier.php",
                properties,
            )
        }
        if (spec.componentEpilog) {
            BitrixTemplateWriter.createFile(
                templateManager,
                defaultTemplateDirectory,
                BitrixTemplateNames.SIMPLE_COMPONENT_TEMPLATE_EPILOG,
                "component_epilog.php",
                properties,
            )
        }

        val langDirectory = componentDirectory.createSubdirectory("lang")
        spec.languages.forEach { language ->
            val languageDirectory = langDirectory.createSubdirectory(language)
            BitrixTemplateWriter.createFiles(
                templateManager,
                languageDirectory,
                BitrixTemplateNames.SIMPLE_COMPONENT_LANGUAGE,
                listOf(".description.php", ".parameters.php"),
                properties,
            )

            val languageTemplateDirectory = languageDirectory
                .createSubdirectory("templates")
                .createSubdirectory(".default")
            BitrixTemplateWriter.createFile(
                templateManager,
                languageTemplateDirectory,
                BitrixTemplateNames.SIMPLE_COMPONENT_LANGUAGE_TEMPLATE,
                "template.php",
                properties,
            )
            if (spec.componentEpilog) {
                BitrixTemplateWriter.createFile(
                    templateManager,
                    languageTemplateDirectory,
                    BitrixTemplateNames.SIMPLE_COMPONENT_LANGUAGE_EPILOG,
                    "component_epilog.php",
                    properties,
                )
            }
        }

        return componentDirectory
    }

    private fun escapePhp(value: String): String = value.replace("\\", "\\\\").replace("'", "\\'")
}
