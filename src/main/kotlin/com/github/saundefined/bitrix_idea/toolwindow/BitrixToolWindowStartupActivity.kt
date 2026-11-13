package com.github.saundefined.bitrix_idea.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class BitrixToolWindowStartupActivity : StartupActivity {
    override fun runActivity(project: Project) {
        BitrixToolWindowManager.updateAvailability(project)
    }
}
