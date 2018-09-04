package de.jl.notificationlog.data

import android.arch.lifecycle.LiveData
import android.arch.paging.DataSource
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query

@Dao
interface NotificationDao {
    @Insert
    fun insertSync(notificationItem: NotificationItem)

    @Query("SELECT * FROM notifications WHERE package = :packageName ORDER BY time ASC")
    fun getNotificationsByApp(packageName: String): DataSource.Factory<Int, NotificationItem>

    @Query("SELECT * FROM notifications ORDER BY time ASC")
    fun getNotificationsOfAllApps(): DataSource.Factory<Int, NotificationItem>

    @Query("SELECT * FROM notifications WHERE package = :packageName ORDER BY time ASC LIMIT :rows OFFSET :offset")
    fun getNotificationsByAppPageSync(packageName: String, rows: Int, offset: Int): List<NotificationItem>

    @Query("SELECT * FROM notifications ORDER BY time ASC LIMIT :rows OFFSET :offset")
    fun getAllNotificationsPageSync(rows: Int, offset: Int): List<NotificationItem>

    @Query("SELECT DISTINCT package FROM notifications ORDER BY package ASC")
    fun getAppsWithNotifications(): LiveData<List<AppWithNotification>>

    @Query("DELETE FROM notifications WHERE package = :packageName")
    fun deleteNotificationsByAppSync(packageName: String)

    @Query("DELETE FROM notifications")
    fun deleteAllNotificationsSync()
}