package com.github.saundefined.bitrix_idea.activity

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.library.BitrixLibraryRootsProvider
import com.github.saundefined.bitrix_idea.roots.BitrixSourceRootsManager
import com.github.saundefined.bitrix_idea.settings.BitrixProjectSettings
import com.github.saundefined.bitrix_idea.toolwindow.BitrixToolWindowManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class BitrixProjectDetectionActivity : StartupActivity {
    override fun runActivity(project: Project) {
        val settings = BitrixProjectSettings.getInstance(project)
        if (settings.bitrixFrameworkEnabled || settings.bitrixDetectionNotificationShown) return

        DumbService.getInstance(project).runWhenSmart {
            if (project.isDisposed) return@runWhenSmart

            ApplicationManager.getApplication().executeOnPooledThread {
                if (project.isDisposed) return@executeOnPooledThread
                if (!BitrixLibraryRootsProvider.hasBitrixDirectories(project)) return@executeOnPooledThread

                ApplicationManager.getApplication().invokeLater {
                    if (project.isDisposed) return@invokeLater

                    val currentSettings = BitrixProjectSettings.getInstance(project)
                    if (currentSettings.bitrixFrameworkEnabled || currentSettings.bitrixDetectionNotificationShown) {
                        return@invokeLater
                    }

                    currentSettings.bitrixDetectionNotificationShown = true
                    showNotification(project, currentSettings)
                }
            }
        }
    }

    private fun showNotification(project: Project, settings: BitrixProjectSettings) {
        val notification = Notification(
            NOTIFICATION_GROUP,
            message("notification.bitrix.detected.title"),
            message("notification.bitrix.detected.content"),
            NotificationType.INFORMATION,
        )

        notification.addAction(object : NotificationAction(message("notification.bitrix.detected.enable")) {
            override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                settings.bitrixFrameworkEnabled = true
                BitrixLibraryRootsProvider.refresh(project)
                BitrixSourceRootsManager.configure(project, force = true)
                BitrixToolWindowManager.updateAvailability(project)
                notification.expire()
            }
        })

        Notifications.Bus.notify(notification, project)
    }

    private companion object {
        const val NOTIFICATION_GROUP = "Bitrix Idea Framework"
    }
}
