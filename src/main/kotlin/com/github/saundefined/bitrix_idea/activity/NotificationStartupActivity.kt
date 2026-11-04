package com.github.saundefined.bitrix_idea.activity

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.notification.NotificationsConfiguration
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class NotificationStartupActivity : StartupActivity {
    override fun runActivity(project: Project) {
        ApplicationManager.getApplication().invokeLater {
            val notification = Notification(
                NOTIFICATION_GROUP,
                message("startup.notification.title"),
                NotificationType.IDE_UPDATE,
            )

            notification.addAction(object : NotificationAction(message("startup.notification.action")) {
                override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                    BrowserUtil.browse(GITHUB_URL)
                }
            })

            notification.addAction(object : NotificationAction(message("startup.notification.dismiss")) {
                override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                    NotificationsConfiguration.getNotificationsConfiguration().changeSettings(
                        NOTIFICATION_GROUP,
                        NotificationDisplayType.NONE,
                        false,
                        false,
                    )
                    notification.expire()
                }
            })

            Notifications.Bus.notify(notification)
        }
    }

    private companion object {
        const val NOTIFICATION_GROUP = "Bitrix Idea Plugin"
        const val GITHUB_URL = "https://github.com/saundefined/bitrix-idea"
    }
}
