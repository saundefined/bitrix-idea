package com.github.saundefined.bitrix_idea.toolwindow

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import icons.BitrixIdeaIcons

class BitrixProjectViewNodeDecorator : ProjectViewNodeDecorator {
    override fun decorate(node: ProjectViewNode<*>, data: PresentationData) {
        val file = node.virtualFile ?: return
        val path = file.path
        val isComponentFile = file.name in setOf("class.php", "component.php", ".description.php", ".parameters.php") &&
            path.contains("/components/")
        val isBitrixFile = isComponentFile ||
            file.name == ".settings.php" ||
            file.name.endsWith("Table.php") ||
            file.name.endsWith("Agent.php") ||
            file.name.endsWith("Handler.php")

        if (isBitrixFile) {
            data.setIcon(BitrixIdeaIcons.Bitrix)
            data.tooltip = "Bitrix Idea"
        }
    }
}
