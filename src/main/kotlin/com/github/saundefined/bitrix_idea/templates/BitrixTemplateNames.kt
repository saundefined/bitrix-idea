package com.github.saundefined.bitrix_idea.templates

/**
 * Полные имена J2EE-шаблонов из `src/main/resources/fileTemplates/j2ee`.
 * Значения должны совпадать с реальными именами `.ft` файлов.
 */
object BitrixTemplateNames {
    const val MODULE = "Bitrix Module"
    const val MODULE_SETTINGS = "$MODULE .settings.php"
    const val MODULE_DEFAULT_OPTION = "$MODULE default_option.php"
    const val MODULE_INCLUDE = "$MODULE include.php"
    const val MODULE_OPTIONS = "$MODULE options.php"
    const val MODULE_PROLOG = "$MODULE prolog.php"
    const val MODULE_INSTALL = "$MODULE install"
    const val MODULE_INSTALL_DB = "$MODULE install db"
    const val MODULE_ADMIN = "$MODULE admin"
    const val MODULE_ADMIN_MENU = "$MODULE_ADMIN menu.php"
    const val MODULE_LANGUAGE_INSTALL = "$MODULE language install"
    const val MODULE_LANGUAGE_ADMIN = "$MODULE language admin"
    const val MODULE_LANGUAGE_ADMIN_MENU = "$MODULE_LANGUAGE_ADMIN menu.php"
    const val MODULE_GITIGNORE = "$MODULE .gitignore"

    const val TEMPLATE = "Bitrix Template"
    const val TEMPLATE_STYLES = "$TEMPLATE .styles.php"
    const val TEMPLATE_DESCRIPTION = "$TEMPLATE description.php"
    const val TEMPLATE_HEADER = "$TEMPLATE header.php"
    const val TEMPLATE_FOOTER = "$TEMPLATE footer.php"
    const val TEMPLATE_STYLES_CSS = "$TEMPLATE styles.css"
    const val TEMPLATE_TEMPLATE_STYLES_CSS = "$TEMPLATE template_styles.css"

    const val SIMPLE_COMPONENT = "Bitrix Simple Component"
    const val SIMPLE_COMPONENT_CLASS = "$SIMPLE_COMPONENT class.php"
    const val SIMPLE_COMPONENT_PARAMETERS = "$SIMPLE_COMPONENT .parameters.php"
    const val SIMPLE_COMPONENT_DESCRIPTION = "$SIMPLE_COMPONENT .description.php"
    const val SIMPLE_COMPONENT_TEMPLATE = "$SIMPLE_COMPONENT templates default template.php"
    const val SIMPLE_COMPONENT_TEMPLATE_SCRIPT = "$SIMPLE_COMPONENT templates default script.js"
    const val SIMPLE_COMPONENT_TEMPLATE_STYLE = "$SIMPLE_COMPONENT templates default style.css"
    const val SIMPLE_COMPONENT_TEMPLATE_RESULT_MODIFIER = "$SIMPLE_COMPONENT templates default result_modifier.php"
    const val SIMPLE_COMPONENT_TEMPLATE_EPILOG = "$SIMPLE_COMPONENT templates default component_epilog.php"
    const val SIMPLE_COMPONENT_LANGUAGE = "$SIMPLE_COMPONENT language"
    const val SIMPLE_COMPONENT_LANGUAGE_PARAMETERS = "$SIMPLE_COMPONENT_LANGUAGE .parameters.php"
    const val SIMPLE_COMPONENT_LANGUAGE_DESCRIPTION = "$SIMPLE_COMPONENT_LANGUAGE .description.php"
    const val SIMPLE_COMPONENT_LANGUAGE_TEMPLATE = "$SIMPLE_COMPONENT_LANGUAGE templates default template.php"
    const val SIMPLE_COMPONENT_LANGUAGE_EPILOG = "$SIMPLE_COMPONENT_LANGUAGE templates default component_epilog.php"

    const val ORM_ENTITY = "Bitrix ORM Entity.php"
}
