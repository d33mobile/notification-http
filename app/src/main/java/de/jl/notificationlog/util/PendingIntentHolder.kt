package de.jl.notificationlog.util

import android.app.PendingIntent

object PendingIntentHolder {
    private val data = mutableMapOf<Long, PendingIntent>()
    private val lock = Any()

    fun read(savedNotificationId: Long) = synchronized(lock) {
        data[savedNotificationId]
    }

    fun save(savedNotificationId: Long, contentIntent: PendingIntent) = synchronized(lock) {
        data[savedNotificationId] = contentIntent
    }
}