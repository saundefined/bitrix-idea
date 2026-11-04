package com.github.saundefined.bitrix_idea.templates.simple_component

import com.github.saundefined.bitrix_idea.templates.BitrixTemplateNames
import com.intellij.ide.fileTemplates.FileTemplateDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory
import icons.BitrixIdeaIcons

class BitrixSimpleComponentFileTemplateGroupFactory : FileTemplateGroupDescriptorFactory {
    override fun getFileTemplatesDescriptor(): FileTemplateGroupDescriptor {
        return FileTemplateGroupDescriptor(BitrixTemplateNames.SIMPLE_COMPONENT, BitrixIdeaIcons.Bitrix).apply {
            addTemplate(FileTemplateDescriptor(BitrixTemplateNames.SIMPLE_COMPONENT_CLASS))
            addTemplate(FileTemplateDescriptor(BitrixTemplateNames.SIMPLE_COMPONENT_PARAMETERS))
            addTemplate(FileTemplateDescriptor(BitrixTemplateNames.SIMPLE_COMPONENT_DESCRIPTION))
            addTemplate(FileTemplateDescriptor(BitrixTemplateNames.SIMPLE_COMPONENT_TEMPLATE))
            addTemplate(FileTemplateDescriptor(BitrixTemplateNames.SIMPLE_COMPONENT_TEMPLATE_SCRIPT))
            addTemplate(FileTemplateDescriptor(BitrixTemplateNames.SIMPLE_COMPONENT_TEMPLATE_STYLE))
            addTemplate(FileTemplateDescriptor(BitrixTemplateNames.SIMPLE_COMPONENT_TEMPLATE_RESULT_MODIFIER))
            addTemplate(FileTemplateDescriptor(BitrixTemplateNames.SIMPLE_COMPONENT_TEMPLATE_EPILOG))
            addTemplate(FileTemplateDescriptor(BitrixTemplateNames.SIMPLE_COMPONENT_LANGUAGE_PARAMETERS))
            addTemplate(FileTemplateDescriptor(BitrixTemplateNames.SIMPLE_COMPONENT_LANGUAGE_DESCRIPTION))
            addTemplate(FileTemplateDescriptor(BitrixTemplateNames.SIMPLE_COMPONENT_LANGUAGE_TEMPLATE))
            addTemplate(FileTemplateDescriptor(BitrixTemplateNames.SIMPLE_COMPONENT_LANGUAGE_EPILOG))
        }
    }
}
