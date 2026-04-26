package de.jl.notificationlog.data

import de.jl.notificationlog.data.dao.ActiveNotificationDao
import de.jl.notificationlog.data.dao.NotificationDao
import de.jl.notificationlog.data.dao.PendingWebhookDeliveryDao

interface Database {
    fun notification(): NotificationDao
    fun activeNotification(): ActiveNotificationDao
    fun pendingWebhookDelivery(): PendingWebhookDeliveryDao
}
