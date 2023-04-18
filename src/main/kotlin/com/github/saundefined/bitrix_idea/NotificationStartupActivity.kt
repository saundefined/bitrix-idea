package com.github.saundefined.bitrix_idea

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.license.CheckLicense
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class NotificationStartupActivity : StartupActivity {
    override fun runActivity(project: Project) {
        val app = ApplicationManager.getApplication()
        app.invokeLater {
            if (CheckLicense.isLicensed == false) {
                val notification = Notification(
                    "Bitrix Idea Plugin",
                    message("startup_notification_title"),
                    message("startup_notification_body"),
                    NotificationType.INFORMATION
                )

                Notifications.Bus.notify(notification)
            }
        }
    }

}
