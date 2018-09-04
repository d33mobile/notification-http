package de.jl.notificationlog.util

import android.content.Context
import android.content.Intent
import android.provider.Settings
import de.jl.notificationlog.R
import de.jl.notificationlog.service.AccessibilityService
import de.jl.notificationlog.service.NotificationListenerService

object ServiceCheckUtil {
    private const val NOTIFICATION_SERVICES_SPLITTER = ":"
    private const val ACCESSIBILITY_SERVICES_SPLITTER = ":"

    fun isNotificationReadingAllowed(context: Context): Boolean {
        if (context.resources.getBoolean(R.bool.enable_notification_listener_service)) {
            return isNotificationListenerServiceEnabled(context)
        } else {
            return isAccessibilityServiceEnabled(context)
        }
    }

    private fun isNotificationListenerServiceEnabled(context: Context): Boolean {
        val notificationListenerName = context.packageName + "/" + NotificationListenerService::class.java.name
        val enabledNotificationListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")

        return enabledNotificationListeners != null && enabledNotificationListeners.split(NOTIFICATION_SERVICES_SPLITTER).contains(notificationListenerName)
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val ACCESSIBILITY_SERVICE_NAME = context.packageName + "/" + AccessibilityService::class.java.name

        try {
            if (Settings.Secure.getInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED) == 1) {
                val enabledAccessibilityServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)

                return enabledAccessibilityServices.split(ACCESSIBILITY_SERVICES_SPLITTER).contains(ACCESSIBILITY_SERVICE_NAME)
            }
        } catch (ex: Settings.SettingNotFoundException) {
            // ignore
        }

        return false
    }

    fun enableService(context: Context) {
        if (context.resources.getBoolean(R.bool.enable_notification_listener_service)) {
            enableNotificationListenerService(context)
        } else {
            enableAccessibilityService(context)
        }
    }

    private fun enableAccessibilityService(context: Context) {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun enableNotificationListenerService(context: Context) {
        context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
    }
}
