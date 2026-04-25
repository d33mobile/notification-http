package de.jl.notificationlog

import android.app.Application
import de.jl.notificationlog.ui.CheckAuthUtil
import de.jl.notificationlog.util.Configuration
import de.jl.notificationlog.util.DeleteOldNotificationsUtil
import de.jl.notificationlog.webhook.WebhookConfig

class Application: Application() {
    val checkAuthUtil = CheckAuthUtil(this)

    override fun onCreate() {
        super.onCreate()

        // init the background job
        DeleteOldNotificationsUtil.with(this)

        // re-drain pending webhook deliveries left over from a previous process / reboot
        if (Configuration.with(this).webhookEnabled) {
            WebhookConfig.enqueue(this)
        }
    }
}
