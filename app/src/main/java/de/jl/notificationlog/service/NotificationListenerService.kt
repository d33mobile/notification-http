package de.jl.notificationlog.service

import android.annotation.TargetApi
import android.service.notification.StatusBarNotification
import android.util.Log

@TargetApi(android.os.Build.VERSION_CODES.JELLY_BEAN_MR2)
class NotificationListenerService : android.service.notification.NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        NotificationSaveUtil.saveNotificationPosted(sbn, this)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        NotificationSaveUtil.saveNotificationRemoved(sbn, this)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()

        val active = activeNotifications.toList()
        NotificationSaveUtil.restoreClickHandlers(active, this)
        // After a reboot the OS does not re-fire onNotificationPosted for what's already in the
        // shade; without this replay the webhook would silently miss the current state.
        NotificationSaveUtil.replayActiveForWebhook(active, this)
    }
}
