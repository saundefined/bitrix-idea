package com.github.saundefined.bitrix_idea.templates.template

import com.github.saundefined.bitrix_idea.templates.BitrixTemplateNames
import com.intellij.ide.fileTemplates.FileTemplateDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory
import icons.BitrixIdeaIcons

class BitrixTemplateFileTemplateGroupFactory : FileTemplateGroupDescriptorFactory {
    override fun getFileTemplatesDescriptor(): FileTemplateGroupDescriptor {
        return FileTemplateGroupDescriptor(BitrixTemplateNames.TEMPLATE, BitrixIdeaIcons.Bitrix).apply {
            addTemplate(FileTemplateDescriptor(BitrixTemplateNames.TEMPLATE_STYLES))
            addTemplate(FileTemplateDescriptor(BitrixTemplateNames.TEMPLATE_DESCRIPTION))
            addTemplate(FileTemplateDescriptor(BitrixTemplateNames.TEMPLATE_HEADER))
            addTemplate(FileTemplateDescriptor(BitrixTemplateNames.TEMPLATE_FOOTER))
            addTemplate(FileTemplateDescriptor(BitrixTemplateNames.TEMPLATE_STYLES_CSS))
            addTemplate(FileTemplateDescriptor(BitrixTemplateNames.TEMPLATE_TEMPLATE_STYLES_CSS))
        }
    }
}
