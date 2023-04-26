package com.github.saundefined.bitrix_idea

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import org.jetbrains.annotations.NotNull
import java.awt.*
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException

class NotificationStartupActivity : StartupActivity {

    @NotNull
    private val NOTIFICATION_GROUP = "Bitrix Idea Plugin"

    private val GITHUB_URL = "https://github.com/saundefined/bitrix-idea"

    override fun runActivity(project: Project) {
        val app = ApplicationManager.getApplication()
        app.invokeLater {
            val notification =
                Notification(NOTIFICATION_GROUP, message("startup.notification.title"), NotificationType.IDE_UPDATE)
            notification.addAction(object :
                NotificationAction(message("startup.notification.action")) {
                override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                    openUrl(GITHUB_URL)
                }
            })

            Notifications.Bus.notify(notification)
        }
    }

    private fun openUrl(url: String) {
        if (Desktop.isDesktopSupported()) {
            val desktop: Desktop = Desktop.getDesktop()
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    val uri = URI(url)
                    desktop.browse(uri)
                } catch (ignored: URISyntaxException) {
                } catch (ignored: IOException) {
                }
            }
        }
    }
}
