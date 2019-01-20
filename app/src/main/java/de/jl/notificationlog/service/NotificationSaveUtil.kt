package de.jl.notificationlog.service

import android.annotation.TargetApi
import android.app.Notification
import android.content.Context
import android.os.Build
import android.service.notification.StatusBarNotification
import de.jl.notificationlog.data.AppDatabase
import de.jl.notificationlog.data.item.ActiveNotificationItem
import de.jl.notificationlog.data.item.NotificationItem
import de.jl.notificationlog.notification.NotificationParser
import de.jl.notificationlog.util.Configuration
import de.jl.notificationlog.util.PendingIntentHolder
import java.util.concurrent.Executors

object NotificationSaveUtil {
    private val saveThread = Executors.newSingleThreadExecutor()

    fun save(notification: Notification, packageName: String, context: Context) {
        if (!Configuration.with(context).shouldLogNotifications(packageName)) {
            return
        }

        val item = NotificationParser.parse(notification, context)
        val database = AppDatabase.with(context)

        saveThread.submit {
            val notificationId = database.notification().insertSync(
                    NotificationItem(
                            // id is auto generated
                            id = 0,
                            packageName = packageName,
                            time = System.currentTimeMillis(),
                            title = item.title,
                            text = item.text,
                            progress = item.progress,
                            progressMax = item.progressMax,
                            progressIndeterminate = item.progressIndeterminate,
                            isOldestVersion = true,
                            isNewestVersion = true
                    )
            )

            // save click action
            PendingIntentHolder.save(
                    savedNotificationId = notificationId,
                    contentIntent = notification.contentIntent
            )
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    fun saveNotificationPosted(notification: StatusBarNotification, context: Context) {
        if (!Configuration.with(context).shouldLogNotifications(notification.packageName)) {
            return
        }

        val item = NotificationParser.parse(notification.notification, context)
        val database = AppDatabase.with(context)

        saveThread.submit {
            database.runInTransaction {
                val activeNotificationItem = database.activeNotification().querySync(
                        appPackageName = notification.packageName,
                        systemId = notification.id,
                        systemTag = prepareTag(notification.tag)
                )

                if (activeNotificationItem == null) {
                    // add new item
                    val notificationId = database.notification().insertSync(
                            NotificationItem(
                                    // id is auto generated
                                    id = 0,
                                    packageName = notification.packageName,
                                    time = System.currentTimeMillis(),
                                    title = item.title,
                                    text = item.text,
                                    progress = item.progress,
                                    progressMax = item.progressMax,
                                    progressIndeterminate = item.progressIndeterminate,
                                    isOldestVersion = true,
                                    isNewestVersion = true
                            )
                    )

                    // add to active notifications
                    database.activeNotification().insertSync(
                            ActiveNotificationItem(
                                    // id is auto generated
                                    id = 0,
                                    appPackageName = notification.packageName,
                                    systemId = notification.id,
                                    systemTag = prepareTag(notification.tag),
                                    previousNotificationItemId = notificationId
                            )
                    )

                    // save click action
                    PendingIntentHolder.save(
                            savedNotificationId = notificationId,
                            contentIntent = notification.notification.contentIntent
                    )
                } else {
                    // update previous item
                    database.notification().setIsNewestNotificationSync(
                            id = activeNotificationItem.previousNotificationItemId,
                            isNewestNotification = false
                    )

                    // add new item
                    val notificationId = database.notification().insertSync(
                            NotificationItem(
                                    // id is auto generated
                                    id = 0,
                                    packageName = notification.packageName,
                                    time = System.currentTimeMillis(),
                                    title = item.title,
                                    text = item.text,
                                    progress = item.progress,
                                    progressMax = item.progressMax,
                                    progressIndeterminate = item.progressIndeterminate,
                                    isOldestVersion = false,
                                    isNewestVersion = true
                            )
                    )

                    // update old notification item
                    database.activeNotification().updateLastNotificationIdSync(
                            activeNotificationId = activeNotificationItem.id,
                            lastNotificationId = notificationId
                    )

                    // save click action
                    PendingIntentHolder.save(
                            savedNotificationId = notificationId,
                            contentIntent = notification.notification.contentIntent
                    )
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    fun saveNotificationRemoved(notification: StatusBarNotification, context: Context) {
        saveNotificationRemoved(
                packageName = notification.packageName,
                notificationId = notification.id,
                notificationTag = notification.tag,
                context = context
        )
    }

    private fun saveNotificationRemoved(packageName: String, notificationId: Int, notificationTag: String?, context: Context) {
        val database = AppDatabase.with(context)

        saveThread.submit {
            database.activeNotification().removeSync(
                    appPackageName = packageName,
                    systemId = notificationId,
                    systemTag = prepareTag(notificationTag)
            )
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    fun restoreClickHandlers(statusBarNotifications: List<StatusBarNotification>, context: Context) {
        val database = AppDatabase.with(context)

        saveThread.submit {
            statusBarNotifications.forEach { notification ->
                database.activeNotification().querySync(
                        appPackageName = notification.packageName,
                        systemTag = prepareTag(notification.tag),
                        systemId = notification.id
                )?.let { activeNotificationItem ->
                    PendingIntentHolder.save(
                            savedNotificationId = activeNotificationItem.previousNotificationItemId,
                            contentIntent = notification.notification.contentIntent
                    )
                }
            }
        }
    }

    private fun prepareTag(tag: String?) = if (tag == null) {
        "null"
    } else {
        "tag:$tag"
    }
}
