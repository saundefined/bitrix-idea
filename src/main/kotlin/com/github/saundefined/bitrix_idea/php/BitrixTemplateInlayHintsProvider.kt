package com.github.saundefined.bitrix_idea.php

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
import com.intellij.codeInsight.hints.ChangeListener
import com.intellij.codeInsight.hints.ImmediateConfigurable
import com.intellij.codeInsight.hints.InlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsProvider
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.NoSettings
import com.intellij.codeInsight.hints.SettingsKey
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Показывает эффективный каталог шаблона рядом с аргументом `IncludeComponent`.
 */
class BitrixTemplateInlayHintsProvider : InlayHintsProvider<NoSettings> {
    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: NoSettings,
        sink: InlayHintsSink,
    ): InlayHintsCollector {
        return object : InlayHintsCollector {
            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
                val literal = element as? StringLiteralExpression ?: return true
                val call = BitrixPhp.directCall(literal) ?: return true
                if (!BitrixPhp.isIncludeComponent(call) || BitrixPhp.argumentIndex(literal) != 1) return true
                val component = call.parameterList?.getParameter(0) as? StringLiteralExpression ?: return true
                val template = literal.contents.ifBlank { ".default" }
                val directories = BitrixProjectStructure.findComponentTemplateDirs(literal.project, component.contents, template)
                val effective = directories.firstOrNull() ?: return true
                val factory = PresentationFactory(editor)
                val relative = effective.path.substringAfter("/local/", effective.path)
                    .substringAfter("/bitrix/", effective.path)
                sink.addInlineElement(
                    literal.textRange.endOffset,
                    false,
                    factory.smallText("  $template: $relative"),
                )
                return true
            }
        }
    }

    override fun createSettings(): NoSettings = NoSettings()

    override val name: String = message("inlay.template.name")

    override val key: SettingsKey<NoSettings> = SettingsKey("bitrix.template.effective")

    override val previewText: String = "bitrix:news template: local/templates/.default/components/bitrix/news"

    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JComponent = JPanel()
        }
    }
}
