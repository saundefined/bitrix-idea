package com.github.saundefined.bitrix_idea.php

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.structure.BitrixProjectStructure
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.psi.elements.FunctionReference
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import icons.BitrixIdeaIcons

class BitrixLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
    ) {
        val literal = element as? StringLiteralExpression
        if (literal != null) {
            val call = BitrixPhp.directCall(literal) ?: return
            val index = BitrixPhp.argumentIndex(literal) ?: return
            val targets = BitrixTargets.resolve(literal, call, index)
            if (targets.isEmpty()) return

            val tooltip = if (BitrixPhp.isIncludeComponent(call) && index == 1) {
                message("line.marker.template")
            } else {
                message("line.marker.tooltip")
            }
            val marker = NavigationGutterIconBuilder.create(BitrixIdeaIcons.Bitrix)
                .setTargets(*targets)
                .setTooltipText(tooltip)
                .createLineMarkerInfo(literal)
            result.add(marker)
            return
        }

        collectTemplateMarker(element, result)
    }

    /**
     * Отдельная иконка на имени `IncludeComponent` открывает реально выбранный шаблон.
     */
    private fun collectTemplateMarker(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
    ) {
        if (element.text != "IncludeComponent") return
        val call = element.parent as? FunctionReference ?: return
        if (!BitrixPhp.isIncludeComponent(call)) return

        val parameters = call.parameterList?.parameters ?: return
        val componentLiteral = parameters.getOrNull(0) as? StringLiteralExpression ?: return
        val componentId = componentLiteral.contents
        if (componentId.isBlank()) return

        val templateName = (parameters.getOrNull(1) as? StringLiteralExpression)?.contents.orEmpty()
        val template = templateName.ifBlank { ".default" }
        val dirs = BitrixProjectStructure.findComponentTemplateDirs(componentLiteral.project, componentId, template)
        val target = dirs.firstNotNullOfOrNull { dir ->
            dir.findChild("template.php")?.let { BitrixTargets.psiFile(componentLiteral.project, it) }
        } ?: dirs.firstNotNullOfOrNull { BitrixTargets.psiDirectory(componentLiteral.project, it) } ?: return

        val marker = NavigationGutterIconBuilder.create(AllIcons.Nodes.Template)
            .setTarget(target)
            .setTooltipText(message("line.marker.open.template", componentId, template))
            .createLineMarkerInfo(element)
        result.add(marker)
    }
}
