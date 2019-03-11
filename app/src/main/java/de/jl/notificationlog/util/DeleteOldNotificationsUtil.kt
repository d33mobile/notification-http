package de.jl.notificationlog.util

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import de.jl.notificationlog.data.AppDatabase
import java.util.concurrent.Executors

class DeleteOldNotificationsUtil (context: Application) {
    companion object {
        private var instance: DeleteOldNotificationsUtil? = null
        private val lock = Object()

        fun with(context: Context): DeleteOldNotificationsUtil {
            if (instance == null) {
                synchronized(lock) {
                    if (instance == null) {
                        instance = DeleteOldNotificationsUtil(context.applicationContext as Application)
                    }
                }
            }

            return instance!!
        }
    }

    private val database = AppDatabase.with(context)
    private val configuration = Configuration.with(context)
    private val deleteThread = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    private fun cleanupDatabaseSync() {
        val daysToKeep = configuration.notificationKeepingDays

        if (daysToKeep <= 0) {
            return
        }

        val now = System.currentTimeMillis()
        val oldestTimestampToKeep = now - daysToKeep * 1000 * 60 * 60 * 24

        database.notification().deleteOldNotificationItemsSync(olderThanTimestamp = oldestTimestampToKeep)
    }

    fun cleanupDatabaseAsync() {
        deleteThread.submit { cleanupDatabaseSync() }
    }

    lateinit private var cleanUpDatabase: Runnable

    init {
        cleanUpDatabase = Runnable {
            cleanupDatabaseAsync()

            handler.postDelayed(cleanUpDatabase, 1000 * 60 * 15 /* 15 minutes */)
        }

        // wait one moment before the first iteration
        // to prevent slowing the app down during the launch
        handler.postDelayed(cleanUpDatabase, 1000 * 10 /* 10 seconds */)
    }
}