package com.github.saundefined.bitrix_idea.toolwindow

import com.github.saundefined.bitrix_idea.settings.BitrixProjectSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import java.util.WeakHashMap

object BitrixHealthToolWindowManager {
    private val panels = WeakHashMap<Project, BitrixHealthPanel>()

    fun register(project: Project, panel: BitrixHealthPanel) {
        panels[project] = panel
    }

    fun refresh(project: Project) {
        panels[project]?.refresh()
    }

    fun updateAvailability(project: Project) {
        val settings = BitrixProjectSettings.getInstance(project)
        val available = settings.bitrixFrameworkEnabled && settings.bitrixHealthWindowEnabled
        ApplicationManager.getApplication().invokeLater {
            if (project.isDisposed) return@invokeLater
            ToolWindowManager.getInstance(project).getToolWindow("BitrixHealth")?.isAvailable = available
        }
    }
}
