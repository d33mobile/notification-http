package de.jl.notificationlog.data

interface Database {
    fun notification(): NotificationDao
}