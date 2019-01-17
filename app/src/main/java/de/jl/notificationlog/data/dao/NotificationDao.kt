package de.jl.notificationlog.data.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import de.jl.notificationlog.data.item.AppWithNotification
import de.jl.notificationlog.data.item.NotificationItem
import de.jl.notificationlog.util.Configuration

@Dao
abstract class NotificationDao {
    @Insert
    abstract fun insertSync(notificationItem: NotificationItem): Long

    @Query("UPDATE NOTIFICATIONS SET is_newest_version = :isNewestNotification WHERE id = :id")
    abstract fun setIsNewestNotificationSync(id: Long, isNewestNotification: Boolean): Long

    fun getNotificationsByApp(packageName: String, sorting: Configuration.Sorting) = when (sorting) {
        Configuration.Sorting.OldestFirst -> getNotificationsByAppAsc(packageName)
        Configuration.Sorting.NewestFirst -> getNotificationsByAppDesc(packageName)
    }

    @Query("SELECT * FROM notifications WHERE package = :packageName ORDER BY time ASC")
    protected abstract fun getNotificationsByAppAsc(packageName: String): DataSource.Factory<Int, NotificationItem>

    @Query("SELECT * FROM notifications WHERE package = :packageName ORDER BY time DESC")
    protected abstract fun getNotificationsByAppDesc(packageName: String): DataSource.Factory<Int, NotificationItem>

    fun getNotificationsOfAllApps(sorting: Configuration.Sorting) = when (sorting) {
        Configuration.Sorting.OldestFirst -> getNotificationsOfAllAppsAsc()
        Configuration.Sorting.NewestFirst -> getNotificationsOfAllAppsDesc()
    }

    @Query("SELECT * FROM notifications ORDER BY time ASC")
    protected abstract fun getNotificationsOfAllAppsAsc(): DataSource.Factory<Int, NotificationItem>

    @Query("SELECT * FROM notifications ORDER BY time DESC")
    protected abstract fun getNotificationsOfAllAppsDesc(): DataSource.Factory<Int, NotificationItem>

    fun getNotificationsByAppPageSync(packageName: String, rows: Int, offset: Int, sorting: Configuration.Sorting) = when (sorting) {
        Configuration.Sorting.OldestFirst -> getNotificationsByAppPageSyncAsc(packageName, rows, offset)
        Configuration.Sorting.NewestFirst -> getNotificationsByAppPageSyncDesc(packageName, rows, offset)
    }

    @Query("SELECT * FROM notifications WHERE package = :packageName ORDER BY time ASC LIMIT :rows OFFSET :offset")
    protected abstract fun getNotificationsByAppPageSyncAsc(packageName: String, rows: Int, offset: Int): List<NotificationItem>

    @Query("SELECT * FROM notifications WHERE package = :packageName ORDER BY time DESC LIMIT :rows OFFSET :offset")
    protected abstract fun getNotificationsByAppPageSyncDesc(packageName: String, rows: Int, offset: Int): List<NotificationItem>

    fun getAllNotificationsPageSync(rows: Int, offset: Int, sorting: Configuration.Sorting) = when (sorting) {
        Configuration.Sorting.OldestFirst -> getAllNotificationsPageSyncAsc(rows, offset)
        Configuration.Sorting.NewestFirst -> getAllNotificationsPageSyncDesc(rows, offset)
    }

    @Query("SELECT * FROM notifications ORDER BY time ASC LIMIT :rows OFFSET :offset")
    protected abstract fun getAllNotificationsPageSyncAsc(rows: Int, offset: Int): List<NotificationItem>

    @Query("SELECT * FROM notifications ORDER BY time DESC LIMIT :rows OFFSET :offset")
    protected abstract fun getAllNotificationsPageSyncDesc(rows: Int, offset: Int): List<NotificationItem>

    @Query("SELECT DISTINCT package FROM notifications ORDER BY package ASC")
    abstract fun getAppsWithNotifications(): LiveData<List<AppWithNotification>>

    @Query("DELETE FROM notifications WHERE package = :packageName")
    abstract fun deleteNotificationsByAppSync(packageName: String)
}
