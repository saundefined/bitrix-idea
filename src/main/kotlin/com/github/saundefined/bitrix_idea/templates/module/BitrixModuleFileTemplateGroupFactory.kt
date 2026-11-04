package com.github.saundefined.bitrix_idea.templates.module

import com.github.saundefined.bitrix_idea.templates.BitrixTemplateNames
import com.intellij.ide.fileTemplates.FileTemplateDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory
import icons.BitrixIdeaIcons

class BitrixModuleFileTemplateGroupFactory : FileTemplateGroupDescriptorFactory {
    override fun getFileTemplatesDescriptor(): FileTemplateGroupDescriptor {
        return FileTemplateGroupDescriptor(BitrixTemplateNames.MODULE, BitrixIdeaIcons.Bitrix).apply {
            addTemplate(FileTemplateDescriptor(BitrixTemplateNames.MODULE_SETTINGS))
            addTemplate(FileTemplateDescriptor(BitrixTemplateNames.MODULE_DEFAULT_OPTION))
            addTemplate(FileTemplateDescriptor(BitrixTemplateNames.MODULE_INCLUDE))
            addTemplate(FileTemplateDescriptor(BitrixTemplateNames.MODULE_OPTIONS))
            addTemplate(FileTemplateDescriptor(BitrixTemplateNames.MODULE_PROLOG))
        }
    }
}
