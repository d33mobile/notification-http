package de.jl.notificationlog.service

import android.annotation.TargetApi
import android.app.Notification
import android.app.Notification.FLAG_ONGOING_EVENT
import android.content.Context
import android.os.Build
import android.service.notification.StatusBarNotification
import de.jl.notificationlog.data.AppDatabase
import de.jl.notificationlog.data.item.ActiveNotificationItem
import de.jl.notificationlog.data.item.NotificationItem
import de.jl.notificationlog.notification.NotificationParser
import de.jl.notificationlog.util.Configuration
import de.jl.notificationlog.util.PendingIntentHolder
import de.jl.notificationlog.webhook.WebhookConfig
import java.util.concurrent.Executors

object NotificationSaveUtil {
    private val saveThread = Executors.newSingleThreadExecutor()

    fun save(notification: Notification, packageName: String, context: Context) {
        if (!Configuration.with(context).shouldLogNotifications(packageName)) {
            return
        }

        val item = NotificationParser.parse(notification, context)
        val database = AppDatabase.with(context)
        val config = Configuration.with(context)
        val webhookEnabled = config.webhookEnabled
        val webhookEligible = webhookEnabled && !shouldSkipForWebhook(notification, config)

        saveThread.submit {
            database.runInTransaction {
                val notificationId = database.notification().insertSyncHandlePossibleDuplicate(
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

                if (webhookEligible) {
                    database.pendingWebhookDelivery().enqueueSync(notificationId)
                }

                // save click action
                PendingIntentHolder.save(
                        savedNotificationId = notificationId,
                        contentIntent = notification.contentIntent
                )
            }

            if (webhookEligible) WebhookConfig.enqueue(context)
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    fun saveNotificationPosted(notification: StatusBarNotification, context: Context) {
        if (!Configuration.with(context).shouldLogNotifications(notification.packageName)) {
            return
        }

        val item = NotificationParser.parse(notification.notification, context)
        val database = AppDatabase.with(context)
        val config = Configuration.with(context)
        val webhookEnabled = config.webhookEnabled
        val webhookEligible = webhookEnabled && !shouldSkipForWebhook(notification.notification, config)

        saveThread.submit {
            database.runInTransaction {
                val activeNotificationItem = database.activeNotification().querySync(
                        appPackageName = notification.packageName,
                        systemId = notification.id,
                        systemTag = prepareTag(notification.tag)
                )

                if (activeNotificationItem == null) {
                    // add new item
                    val notificationId = database.notification().insertSyncHandlePossibleDuplicate(
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

                    if (webhookEligible) {
                        database.pendingWebhookDelivery().enqueueSync(notificationId)
                    }

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
                    val notificationId = database.notification().insertSyncHandlePossibleDuplicate(
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

                    // update old notification item
                    database.activeNotification().updateLastNotificationIdSync(
                            activeNotificationId = activeNotificationItem.id,
                            lastNotificationId = notificationId
                    )

                    if (webhookEligible) {
                        database.pendingWebhookDelivery().enqueueSync(notificationId)
                    }

                    // save click action
                    PendingIntentHolder.save(
                            savedNotificationId = notificationId,
                            contentIntent = notification.notification.contentIntent
                    )
                }
            }

            if (webhookEligible) WebhookConfig.enqueue(context)
        }
    }

    /**
     * When `webhookSkipOngoing` is set (default), suppress webhook delivery for notifications
     * that an app marked as "ongoing" (foreground service heartbeats: download progress,
     * step counters, music players, torrent throughput…). These tick many times per second
     * and would otherwise spam the webhook endpoint. The notification still goes into the
     * local log — only HTTP delivery is skipped.
     */
    private fun shouldSkipForWebhook(notification: Notification, config: Configuration): Boolean {
        if (!config.webhookSkipOngoing) return false
        return (notification.flags and FLAG_ONGOING_EVENT) != 0
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
