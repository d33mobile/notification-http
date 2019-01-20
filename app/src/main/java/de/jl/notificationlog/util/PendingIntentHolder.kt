package de.jl.notificationlog.util

import android.app.PendingIntent
import androidx.collection.LruCache

object PendingIntentHolder {
    private val data = LruCache<Long, PendingIntent>(64)
    private val lock = Any()

    fun read(savedNotificationId: Long) = synchronized(lock) {
        data[savedNotificationId]
    }

    fun save(savedNotificationId: Long, contentIntent: PendingIntent) = synchronized(lock) {
        data.put(savedNotificationId, contentIntent)
    }
}