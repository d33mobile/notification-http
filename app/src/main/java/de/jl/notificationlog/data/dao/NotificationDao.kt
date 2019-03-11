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
    abstract fun setIsNewestNotificationSync(id: Long, isNewestNotification: Boolean)

    fun getNotificationsByApp(packageName: String, sorting: Configuration.Sorting, versionHandling: Configuration.VersionHandling) = when (versionHandling) {
        Configuration.VersionHandling.ShowAllVersions -> when (sorting) {
            Configuration.Sorting.OldestFirst -> getNotificationsByAppAscAllVersions(packageName)
            Configuration.Sorting.NewestFirst -> getNotificationsByAppDescAllVersions(packageName)
        }
        Configuration.VersionHandling.ShowOldestVersionOnly -> when (sorting) {
            Configuration.Sorting.OldestFirst -> getNotificationsByAppAscOldestVersion(packageName)
            Configuration.Sorting.NewestFirst -> getNotificationsByAppDescOldestVersion(packageName)
        }
        Configuration.VersionHandling.ShowNewestVersionOnly -> when (sorting) {
            Configuration.Sorting.OldestFirst -> getNotificationsByAppAscNewestVersion(packageName)
            Configuration.Sorting.NewestFirst -> getNotificationsByAppDescNewestVersion(packageName)
        }
    }

    @Query("SELECT * FROM notifications WHERE package = :packageName ORDER BY time ASC")
    protected abstract fun getNotificationsByAppAscAllVersions(packageName: String): DataSource.Factory<Int, NotificationItem>

    @Query("SELECT * FROM notifications WHERE package = :packageName ORDER BY time DESC")
    protected abstract fun getNotificationsByAppDescAllVersions(packageName: String): DataSource.Factory<Int, NotificationItem>

    @Query("SELECT * FROM notifications WHERE package = :packageName AND is_newest_version = 1 ORDER BY time ASC")
    protected abstract fun getNotificationsByAppAscNewestVersion(packageName: String): DataSource.Factory<Int, NotificationItem>

    @Query("SELECT * FROM notifications WHERE package = :packageName AND is_newest_version = 1 ORDER BY time DESC")
    protected abstract fun getNotificationsByAppDescNewestVersion(packageName: String): DataSource.Factory<Int, NotificationItem>

    @Query("SELECT * FROM notifications WHERE package = :packageName AND is_oldest_version = 1 ORDER BY time ASC")
    protected abstract fun getNotificationsByAppAscOldestVersion(packageName: String): DataSource.Factory<Int, NotificationItem>

    @Query("SELECT * FROM notifications WHERE package = :packageName AND is_oldest_version = 1 ORDER BY time DESC")
    protected abstract fun getNotificationsByAppDescOldestVersion(packageName: String): DataSource.Factory<Int, NotificationItem>

    fun getNotificationsOfAllApps(sorting: Configuration.Sorting, versionHandling: Configuration.VersionHandling) = when(versionHandling) {
        Configuration.VersionHandling.ShowAllVersions -> when (sorting) {
            Configuration.Sorting.OldestFirst -> getNotificationsOfAllAppsAscAllVersions()
            Configuration.Sorting.NewestFirst -> getNotificationsOfAllAppsDescAllVersions()
        }
        Configuration.VersionHandling.ShowOldestVersionOnly -> when (sorting) {
            Configuration.Sorting.OldestFirst -> getNotificationsOfAllAppsAscOldestVersions()
            Configuration.Sorting.NewestFirst -> getNotificationsOfAllAppsDescOldestVersions()
        }
        Configuration.VersionHandling.ShowNewestVersionOnly -> when (sorting) {
            Configuration.Sorting.OldestFirst -> getNotificationsOfAllAppsAscNewestVersions()
            Configuration.Sorting.NewestFirst -> getNotificationsOfAllAppsDescNewestVersions()
        }
    }

    @Query("SELECT * FROM notifications ORDER BY time ASC")
    protected abstract fun getNotificationsOfAllAppsAscAllVersions(): DataSource.Factory<Int, NotificationItem>

    @Query("SELECT * FROM notifications ORDER BY time DESC")
    protected abstract fun getNotificationsOfAllAppsDescAllVersions(): DataSource.Factory<Int, NotificationItem>

    @Query("SELECT * FROM notifications WHERE is_oldest_version = 1 ORDER BY time ASC")
    protected abstract fun getNotificationsOfAllAppsAscOldestVersions(): DataSource.Factory<Int, NotificationItem>

    @Query("SELECT * FROM notifications WHERE is_oldest_version = 1 ORDER BY time DESC")
    protected abstract fun getNotificationsOfAllAppsDescOldestVersions(): DataSource.Factory<Int, NotificationItem>

    @Query("SELECT * FROM notifications WHERE is_newest_version = 1 ORDER BY time ASC")
    protected abstract fun getNotificationsOfAllAppsAscNewestVersions(): DataSource.Factory<Int, NotificationItem>

    @Query("SELECT * FROM notifications WHERE is_newest_version = 1 ORDER BY time DESC")
    protected abstract fun getNotificationsOfAllAppsDescNewestVersions(): DataSource.Factory<Int, NotificationItem>

    fun getNotificationsByAppPageSync(packageName: String, rows: Int, offset: Int, sorting: Configuration.Sorting, versionHandling: Configuration.VersionHandling) = when (versionHandling) {
        Configuration.VersionHandling.ShowAllVersions -> when (sorting) {
            Configuration.Sorting.OldestFirst -> getNotificationsByAppPageSyncAscAllVersions(packageName, rows, offset)
            Configuration.Sorting.NewestFirst -> getNotificationsByAppPageSyncDescAllVersions(packageName, rows, offset)
        }
        Configuration.VersionHandling.ShowOldestVersionOnly-> when (sorting) {
            Configuration.Sorting.OldestFirst -> getNotificationsByAppPageSyncAscOldestVersion(packageName, rows, offset)
            Configuration.Sorting.NewestFirst -> getNotificationsByAppPageSyncDescOldestVersion(packageName, rows, offset)
        }
        Configuration.VersionHandling.ShowNewestVersionOnly -> when (sorting) {
            Configuration.Sorting.OldestFirst -> getNotificationsByAppPageSyncAscNewestVersion(packageName, rows, offset)
            Configuration.Sorting.NewestFirst -> getNotificationsByAppPageSyncDescNewestVersion(packageName, rows, offset)
        }
    }

    @Query("SELECT * FROM notifications WHERE package = :packageName ORDER BY time ASC LIMIT :rows OFFSET :offset")
    protected abstract fun getNotificationsByAppPageSyncAscAllVersions(packageName: String, rows: Int, offset: Int): List<NotificationItem>

    @Query("SELECT * FROM notifications WHERE package = :packageName ORDER BY time DESC LIMIT :rows OFFSET :offset")
    protected abstract fun getNotificationsByAppPageSyncDescAllVersions(packageName: String, rows: Int, offset: Int): List<NotificationItem>

    @Query("SELECT * FROM notifications WHERE package = :packageName AND is_oldest_version = 1 ORDER BY time ASC LIMIT :rows OFFSET :offset")
    protected abstract fun getNotificationsByAppPageSyncAscOldestVersion(packageName: String, rows: Int, offset: Int): List<NotificationItem>

    @Query("SELECT * FROM notifications WHERE package = :packageName AND is_oldest_version = 1 ORDER BY time DESC LIMIT :rows OFFSET :offset")
    protected abstract fun getNotificationsByAppPageSyncDescOldestVersion(packageName: String, rows: Int, offset: Int): List<NotificationItem>

    @Query("SELECT * FROM notifications WHERE package = :packageName AND is_newest_version = 1 ORDER BY time ASC LIMIT :rows OFFSET :offset")
    protected abstract fun getNotificationsByAppPageSyncAscNewestVersion(packageName: String, rows: Int, offset: Int): List<NotificationItem>

    @Query("SELECT * FROM notifications WHERE package = :packageName AND is_newest_version = 1 ORDER BY time DESC LIMIT :rows OFFSET :offset")
    protected abstract fun getNotificationsByAppPageSyncDescNewestVersion(packageName: String, rows: Int, offset: Int): List<NotificationItem>

    fun getAllNotificationsPageSync(rows: Int, offset: Int, sorting: Configuration.Sorting, versionHandling: Configuration.VersionHandling) = when (versionHandling) {
        Configuration.VersionHandling.ShowAllVersions -> when (sorting) {
            Configuration.Sorting.OldestFirst -> getAllNotificationsPageSyncAscAllVersions(rows, offset)
            Configuration.Sorting.NewestFirst -> getAllNotificationsPageSyncDescAllVersions(rows, offset)
        }
        Configuration.VersionHandling.ShowOldestVersionOnly -> when (sorting) {
            Configuration.Sorting.OldestFirst -> getAllNotificationsPageSyncAscOldestVersion(rows, offset)
            Configuration.Sorting.NewestFirst -> getAllNotificationsPageSyncDescOldestVersion(rows, offset)
        }
        Configuration.VersionHandling.ShowNewestVersionOnly -> when (sorting) {
            Configuration.Sorting.OldestFirst -> getAllNotificationsPageSyncAscNewestVersion(rows, offset)
            Configuration.Sorting.NewestFirst -> getAllNotificationsPageSyncDescNewestVersion(rows, offset)
        }
    }

    @Query("SELECT * FROM notifications ORDER BY time ASC LIMIT :rows OFFSET :offset")
    protected abstract fun getAllNotificationsPageSyncAscAllVersions(rows: Int, offset: Int): List<NotificationItem>

    @Query("SELECT * FROM notifications ORDER BY time DESC LIMIT :rows OFFSET :offset")
    protected abstract fun getAllNotificationsPageSyncDescAllVersions(rows: Int, offset: Int): List<NotificationItem>

    @Query("SELECT * FROM notifications WHERE is_oldest_version = 1 ORDER BY time ASC LIMIT :rows OFFSET :offset")
    protected abstract fun getAllNotificationsPageSyncAscOldestVersion(rows: Int, offset: Int): List<NotificationItem>

    @Query("SELECT * FROM notifications WHERE is_oldest_version = 1 ORDER BY time DESC LIMIT :rows OFFSET :offset")
    protected abstract fun getAllNotificationsPageSyncDescOldestVersion(rows: Int, offset: Int): List<NotificationItem>

    @Query("SELECT * FROM notifications WHERE is_newest_version = 1 ORDER BY time ASC LIMIT :rows OFFSET :offset")
    protected abstract fun getAllNotificationsPageSyncAscNewestVersion(rows: Int, offset: Int): List<NotificationItem>

    @Query("SELECT * FROM notifications WHERE is_newest_version = 1 ORDER BY time DESC LIMIT :rows OFFSET :offset")
    protected abstract fun getAllNotificationsPageSyncDescNewestVersion(rows: Int, offset: Int): List<NotificationItem>

    @Query("SELECT DISTINCT package FROM notifications ORDER BY package ASC")
    abstract fun getAppsWithNotifications(): LiveData<List<AppWithNotification>>

    @Query("DELETE FROM notifications WHERE package = :packageName")
    abstract fun deleteNotificationsByAppSync(packageName: String)

    @Query("DELETE FROM notifications WHERE time < :olderThanTimestamp")
    abstract fun deleteOldNotificationItemsSync(olderThanTimestamp: Long)
}
