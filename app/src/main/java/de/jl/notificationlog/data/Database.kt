package de.jl.notificationlog.data

import de.jl.notificationlog.data.dao.ActiveNotificationDao
import de.jl.notificationlog.data.dao.NotificationDao

interface Database {
    fun notification(): NotificationDao
    fun activeNotification(): ActiveNotificationDao
}