package de.jl.notificationlog.service

import android.annotation.TargetApi
import android.service.notification.StatusBarNotification
import android.util.Log

import de.jl.notificationlog.BuildConfig

@TargetApi(android.os.Build.VERSION_CODES.JELLY_BEAN_MR2)
class NotificationListenerService : android.service.notification.NotificationListenerService() {
    companion object {
        private val LOG_TAG = "NotificationListenerSer"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "onNotificationPosted")
        }

        NotificationSaveUtil.saveNotificationPosted(sbn, this)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "onNotificationRemoved")
        }

        NotificationSaveUtil.saveNotificationRemoved(sbn, this)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()

        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "onListenerConnected")
        }

        NotificationSaveUtil.restoreClickHandlers(activeNotifications.toList(), this)
    }
}
