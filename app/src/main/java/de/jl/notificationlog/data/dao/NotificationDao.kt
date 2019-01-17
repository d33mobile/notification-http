package de.jl.notificationlog.data.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import de.jl.notificationlog.data.item.AppWithNotification
import de.jl.notificationlog.data.item.NotificationItem

@Dao
interface NotificationDao {
    @Insert
    fun insertSync(notificationItem: NotificationItem): Long

    @Query("UPDATE NOTIFICATIONS SET is_newest_version = :isNewestNotification WHERE id = :id")
    fun setIsNewestNotificationSync(id: Long, isNewestNotification: Boolean): Long

    @Query("SELECT * FROM notifications WHERE package = :packageName ORDER BY time ASC")
    fun getNotificationsByAppAsc(packageName: String): DataSource.Factory<Int, NotificationItem>

    @Query("SELECT * FROM notifications ORDER BY time ASC")
    fun getNotificationsOfAllAppsAsc(): DataSource.Factory<Int, NotificationItem>

    @Query("SELECT * FROM notifications WHERE package = :packageName ORDER BY time DESC")
    fun getNotificationsByAppDesc(packageName: String): DataSource.Factory<Int, NotificationItem>

    @Query("SELECT * FROM notifications ORDER BY time DESC")
    fun getNotificationsOfAllAppsDesc(): DataSource.Factory<Int, NotificationItem>

    @Query("SELECT * FROM notifications WHERE package = :packageName ORDER BY time ASC LIMIT :rows OFFSET :offset")
    fun getNotificationsByAppPageSyncAsc(packageName: String, rows: Int, offset: Int): List<NotificationItem>

    @Query("SELECT * FROM notifications WHERE package = :packageName ORDER BY time DESC LIMIT :rows OFFSET :offset")
    fun getNotificationsByAppPageSyncDesc(packageName: String, rows: Int, offset: Int): List<NotificationItem>

    @Query("SELECT * FROM notifications ORDER BY time ASC LIMIT :rows OFFSET :offset")
    fun getAllNotificationsPageSyncAsc(rows: Int, offset: Int): List<NotificationItem>

    @Query("SELECT * FROM notifications ORDER BY time DESC LIMIT :rows OFFSET :offset")
    fun getAllNotificationsPageSyncDesc(rows: Int, offset: Int): List<NotificationItem>

    @Query("SELECT DISTINCT package FROM notifications ORDER BY package ASC")
    fun getAppsWithNotifications(): LiveData<List<AppWithNotification>>

    @Query("DELETE FROM notifications WHERE package = :packageName")
    fun deleteNotificationsByAppSync(packageName: String)
}
