package com.github.saundefined.bitrix_idea.templates.gitignore

import icons.BitrixIdeaIcons
import com.intellij.ide.fileTemplates.FileTemplateDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory

class BitrixModuleFileTemplateGroupFactory : FileTemplateGroupDescriptorFactory {
    override fun getFileTemplatesDescriptor(): FileTemplateGroupDescriptor {
        val group = FileTemplateGroupDescriptor("Bitrix Module", BitrixIdeaIcons.Bitrix)
        group.addTemplate(FileTemplateDescriptor(BITRIX_MODULE_GITIGNORE_TEMPLATE))

        return group
    }

    companion object {
        const val BITRIX_MODULE_GITIGNORE_TEMPLATE = "Bitrix Module .gitignore"
    }
}