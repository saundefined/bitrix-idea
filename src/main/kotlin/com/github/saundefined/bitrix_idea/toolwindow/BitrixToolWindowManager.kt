package com.github.saundefined.bitrix_idea.toolwindow

import com.github.saundefined.bitrix_idea.settings.BitrixProjectSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import java.util.WeakHashMap

/**
 * Держит ссылки на открытые панели Bitrix, чтобы обновлять их после смены настроек.
 */
object BitrixToolWindowManager {
    private val panels = WeakHashMap<Project, BitrixTreePanel>()

    fun register(project: Project, panel: BitrixTreePanel) {
        panels[project] = panel
    }

    fun refresh(project: Project) {
        panels[project]?.refresh()
    }

    /**
     * Полностью убирает пункт Bitrix из списка окон, если framework или окно выключены.
     */
    fun updateAvailability(project: Project) {
        val settings = BitrixProjectSettings.getInstance(project)
        val available = settings.bitrixFrameworkEnabled && settings.bitrixToolWindowEnabled
        ApplicationManager.getApplication().invokeLater {
            if (project.isDisposed) return@invokeLater
            ToolWindowManager.getInstance(project).getToolWindow("Bitrix")?.isAvailable = available
        }
    }
}
