package com.github.saundefined.bitrix_idea.roots

import com.github.saundefined.bitrix_idea.settings.BitrixProjectSettings
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class BitrixSourceRootsStartupActivity : StartupActivity {
    override fun runActivity(project: Project) {
        if (!BitrixProjectSettings.getInstance(project).bitrixFrameworkEnabled) return
        DumbService.getInstance(project).runWhenSmart {
            if (!project.isDisposed) {
                BitrixSourceRootsManager.configure(project)
            }
        }
    }
}
