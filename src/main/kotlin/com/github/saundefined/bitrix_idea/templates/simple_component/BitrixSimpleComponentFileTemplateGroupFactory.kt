package com.github.saundefined.bitrix_idea.templates.simple_component

import icons.BitrixIdeaIcons
import com.intellij.ide.fileTemplates.FileTemplateDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory

class BitrixSimpleComponentFileTemplateGroupFactory : FileTemplateGroupDescriptorFactory {
    override fun getFileTemplatesDescriptor(): FileTemplateGroupDescriptor {
        val group = FileTemplateGroupDescriptor("Bitrix Simple Component", BitrixIdeaIcons.Bitrix)
        group.addTemplate(FileTemplateDescriptor(BITRIX_SIMPLE_COMPONENT_CLASS_TEMPLATE))
        group.addTemplate(FileTemplateDescriptor(BITRIX_SIMPLE_COMPONENT_PARAMETERS_TEMPLATE))
        group.addTemplate(FileTemplateDescriptor(BITRIX_SIMPLE_COMPONENT_DESCRIPTION_TEMPLATE))
        group.addTemplate(FileTemplateDescriptor(BITRIX_SIMPLE_COMPONENT_TEMPLATES_DEFAULT_TEMPLATE_TEMPLATE))
        group.addTemplate(FileTemplateDescriptor(BITRIX_SIMPLE_COMPONENT_LANGUAGE__PARAMETERS_TEMPLATE))
        group.addTemplate(FileTemplateDescriptor(BITRIX_SIMPLE_COMPONENT_LANGUAGE_DESCRIPTION_TEMPLATE))
        group.addTemplate(FileTemplateDescriptor(BITRIX_SIMPLE_COMPONENT_LANGUAGE_TEMPLATES_DEFAULT_TEMPLATE_TEMPLATE))

        return group
    }

    companion object {
        const val BITRIX_SIMPLE_COMPONENT_CLASS_TEMPLATE = "Bitrix Simple Component class.php"
        const val BITRIX_SIMPLE_COMPONENT_PARAMETERS_TEMPLATE = "Bitrix Simple Component .parameters.php"
        const val BITRIX_SIMPLE_COMPONENT_DESCRIPTION_TEMPLATE = "Bitrix Simple Component .description.php"
        const val BITRIX_SIMPLE_COMPONENT_TEMPLATES_DEFAULT_TEMPLATE_TEMPLATE =
            "Bitrix Simple Component templates default template.php"
        const val BITRIX_SIMPLE_COMPONENT_LANGUAGE__PARAMETERS_TEMPLATE =
            "Bitrix Simple Component language .parameters.php"
        const val BITRIX_SIMPLE_COMPONENT_LANGUAGE_DESCRIPTION_TEMPLATE =
            "Bitrix Simple Component language .description.php"
        const val BITRIX_SIMPLE_COMPONENT_LANGUAGE_TEMPLATES_DEFAULT_TEMPLATE_TEMPLATE =
            "Bitrix Simple Component language templates default template.php"
    }
}