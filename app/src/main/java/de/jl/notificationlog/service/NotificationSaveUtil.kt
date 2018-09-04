package de.jl.notificationlog.service

import android.app.Notification
import android.content.Context
import de.jl.notificationlog.data.AppDatabase
import de.jl.notificationlog.data.NotificationItem
import de.jl.notificationlog.notification.NotificationParser
import java.util.concurrent.Executors

object NotificationSaveUtil {
    private val saveThread = Executors.newSingleThreadExecutor()

    fun save(notification: Notification, packageName: String, context: Context) {
        val item = NotificationParser.parse(notification, context)

        save(NotificationItem(
                id = 0,
                packageName = packageName,
                time = System.currentTimeMillis(),
                title = item.title,
                text = item.text
        ), context)
    }

    fun save(notificationItem: NotificationItem, context: Context) {
        val database = AppDatabase.with(context)

        saveThread.submit {
            database.notification().insertSync(notificationItem)
        }
    }
}