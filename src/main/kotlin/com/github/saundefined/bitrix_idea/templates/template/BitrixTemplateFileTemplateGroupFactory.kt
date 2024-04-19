package com.github.saundefined.bitrix_idea.templates.template

import icons.BitrixIdeaIcons
import com.intellij.ide.fileTemplates.FileTemplateDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory

class BitrixTemplateFileTemplateGroupFactory : FileTemplateGroupDescriptorFactory {
    override fun getFileTemplatesDescriptor(): FileTemplateGroupDescriptor {
        val group = FileTemplateGroupDescriptor("Bitrix Template", BitrixIdeaIcons.Bitrix)
        group.addTemplate(FileTemplateDescriptor(BITRIX_TEMPLATE_STYLES_TEMPLATE))
        group.addTemplate(FileTemplateDescriptor(BITRIX_TEMPLATE_DESCRIPTION_TEMPLATE))
        group.addTemplate(FileTemplateDescriptor(BITRIX_TEMPLATE_HEADER_TEMPLATE))
        group.addTemplate(FileTemplateDescriptor(BITRIX_TEMPLATE_FOOTER_TEMPLATE))
        group.addTemplate(FileTemplateDescriptor(BITRIX_TEMPLATE_STYLES_CSS_TEMPLATE))
        group.addTemplate(FileTemplateDescriptor(BITRIX_TEMPLATE_TEMPLATE_STYLES_CSS_TEMPLATE))

        return group
    }

    companion object {
        const val BITRIX_TEMPLATE_STYLES_TEMPLATE = "Bitrix Template .styles.php"
        const val BITRIX_TEMPLATE_DESCRIPTION_TEMPLATE = "Bitrix Template description.php"
        const val BITRIX_TEMPLATE_HEADER_TEMPLATE = "Bitrix Template header.php"
        const val BITRIX_TEMPLATE_FOOTER_TEMPLATE = "Bitrix Template footer.php"
        const val BITRIX_TEMPLATE_STYLES_CSS_TEMPLATE = "Bitrix Template styles.css"
        const val BITRIX_TEMPLATE_TEMPLATE_STYLES_CSS_TEMPLATE = "Bitrix Template template_styles.css"
    }
}