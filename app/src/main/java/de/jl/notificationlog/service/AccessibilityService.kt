package de.jl.notificationlog.service

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Notification
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import de.jl.notificationlog.BuildConfig

class AccessibilityService : android.accessibilityservice.AccessibilityService() {
    companion object {
        private val LOG_TAG = "AccessibilityService"
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "onAccessibilityEvent")
        }

        if (event.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            val packageName = event.packageName.toString()
            if (event.parcelableData != null && event.parcelableData is Notification) {
                if (BuildConfig.DEBUG) {
                    Log.d(LOG_TAG, "onNotification")
                }

                val notification = event.parcelableData as Notification

                NotificationSaveUtil.save(notification, packageName, this)
            }
        }
    }

    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.notificationTimeout = 20
        serviceInfo = info

        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "onServiceConnected")
        }
    }

    override fun onInterrupt() {
        Log.d(LOG_TAG, "onInterrupt")
    }
}
