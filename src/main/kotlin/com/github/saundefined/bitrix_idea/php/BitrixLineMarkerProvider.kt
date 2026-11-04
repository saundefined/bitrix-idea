package com.github.saundefined.bitrix_idea.php

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import icons.BitrixIdeaIcons

class BitrixLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
    ) {
        val literal = element as? StringLiteralExpression ?: return
        val call = BitrixPhp.directCall(literal) ?: return
        val index = BitrixPhp.argumentIndex(literal) ?: return
        val targets = BitrixTargets.resolve(literal, call, index)
        if (targets.isEmpty()) return

        val marker = NavigationGutterIconBuilder.create(BitrixIdeaIcons.Bitrix)
            .setTargets(*targets)
            .setTooltipText(message("line.marker.tooltip"))
            .createLineMarkerInfo(literal)
        result.add(marker)
    }
}
