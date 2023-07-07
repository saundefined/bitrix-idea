package com.github.saundefined.bitrix_idea.templates.module

import com.github.saundefined.bitrix_idea.icons.BitrixIdeaIcons
import com.intellij.ide.fileTemplates.FileTemplateDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory

class BitrixModuleFileTemplateGroupFactory : FileTemplateGroupDescriptorFactory {
    override fun getFileTemplatesDescriptor(): FileTemplateGroupDescriptor {
        val group = FileTemplateGroupDescriptor("Bitrix Module", BitrixIdeaIcons.Bitrix)
        group.addTemplate(FileTemplateDescriptor(BITRIX_MODULE_SETTINGS_TEMPLATE))
        group.addTemplate(FileTemplateDescriptor(BITRIX_MODULE_DEFAULT_OPTION_TEMPLATE))
        group.addTemplate(FileTemplateDescriptor(BITRIX_MODULE_INCLUDE_TEMPLATE))
        group.addTemplate(FileTemplateDescriptor(BITRIX_MODULE_OPTIONS_TEMPLATE))
        group.addTemplate(FileTemplateDescriptor(BITRIX_MODULE_PROLOG_TEMPLATE))

        return group
    }

    companion object {
        const val BITRIX_MODULE_SETTINGS_TEMPLATE = "Bitrix Module .settings.php"
        const val BITRIX_MODULE_DEFAULT_OPTION_TEMPLATE = "Bitrix Module default_option.php"
        const val BITRIX_MODULE_INCLUDE_TEMPLATE = "Bitrix Module include.php"
        const val BITRIX_MODULE_OPTIONS_TEMPLATE = "Bitrix Module options.php"
        const val BITRIX_MODULE_PROLOG_TEMPLATE = "Bitrix Module prolog.php"
    }
}