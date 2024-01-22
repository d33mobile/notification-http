package de.jl.notificationlog.service

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Notification
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class AccessibilityService : android.accessibilityservice.AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            val packageName = event.packageName.toString()
            if (event.parcelableData != null && event.parcelableData is Notification) {
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
    }

    override fun onInterrupt() {
        // nothing to do
    }
}
