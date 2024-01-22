package de.jl.notificationlog.util

import android.app.PendingIntent
import android.util.Log

object PendingIntentHolder {
    private const val LOG_TAG = "PendingIntentHolder"
    private const val CACHE_SIZE = 64

    private var nextCachedIntentId = 0
    private val cachedIntents = mutableMapOf<Int, PendingIntent>()
    private val savedNotificationIdToCachedIntent = mutableMapOf<Long, Int>()
    private val lock = Any()

    fun read(savedNotificationId: Long): PendingIntent? = synchronized(lock) {
        val intentId = savedNotificationIdToCachedIntent[savedNotificationId]

        return if (intentId == null) {
            null
        } else {
            cachedIntents[intentId]
        }
    }

    fun save(savedNotificationId: Long, contentIntent: PendingIntent) = synchronized(lock) {
        // check if intent already exists
        val oldCachedIntentId = cachedIntents.entries.find { it.value == contentIntent }?.key

        if (oldCachedIntentId != null) {
            savedNotificationIdToCachedIntent[savedNotificationId] = oldCachedIntentId
        } else {
            // add intent
            val newCachedIntentId = nextCachedIntentId++
            cachedIntents[newCachedIntentId] = contentIntent
            savedNotificationIdToCachedIntent[savedNotificationId] = newCachedIntentId

            // eventually remove old intent
            if (cachedIntents.size > CACHE_SIZE) {
                val oldestCachedIntentId = cachedIntents.keys.minOrNull()
                cachedIntents.remove(oldestCachedIntentId)

                // remove referencing items
                savedNotificationIdToCachedIntent
                        .entries
                        .filter { it.value == oldestCachedIntentId }
                        .forEach {
                            savedNotificationIdToCachedIntent.remove(it.key)
                        }
            }
        }
    }
}