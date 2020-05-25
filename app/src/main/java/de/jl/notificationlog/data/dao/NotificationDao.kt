package de.jl.notificationlog.data.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import de.jl.notificationlog.data.item.AppWithNotification
import de.jl.notificationlog.data.item.NotificationItem
import de.jl.notificationlog.util.Configuration

@Dao
abstract class NotificationDao {
    @Insert
    abstract fun insertSync(notificationItem: NotificationItem): Long

    @Query("UPDATE NOTIFICATIONS SET is_newest_version = :isNewestNotification WHERE id = :id")
    abstract fun setIsNewestNotificationSync(id: Long, isNewestNotification: Boolean)

    @RawQuery(observedEntities = [ NotificationItem::class ])
    protected abstract fun getNotificationsLive(query: SupportSQLiteQuery): DataSource.Factory<Int, NotificationItem>

    @RawQuery(observedEntities = [ NotificationItem::class ])
    protected abstract fun getNotificationsSync(query: SupportSQLiteQuery): List<NotificationItem>

    fun getNotifications(packageName: String?, sorting: Configuration.Sorting, versionHandling: Configuration.VersionHandling) = getNotificationsLive(
            buildSelectQuery(
                    packageName = packageName,
                    sorting = sorting,
                    versionHandling = versionHandling,
                    limit = null
            )
    )

    fun getNotificationsPageSync(packageName: String?, rows: Int, offset: Int, sorting: Configuration.Sorting, versionHandling: Configuration.VersionHandling) = getNotificationsSync(
            buildSelectQuery(
                    packageName = packageName,
                    sorting = sorting,
                    versionHandling = versionHandling,
                    limit = "$offset,$rows"
            )
    )

    private fun buildSelectQuery(
            packageName: String?, sorting: Configuration.Sorting, versionHandling: Configuration.VersionHandling,
            limit: String?
    ) = SupportSQLiteQueryBuilder.builder("notifications")
            .apply {
                val conditions = mutableListOf<String>()
                val conditionArgs = mutableListOf<String>()

                if (packageName != null) {
                    conditions.add("package = ?"); conditionArgs.add(packageName)
                }

                if (conditions.isNotEmpty()) {
                    selection(conditions.joinToString(separator = " AND "), conditionArgs.toTypedArray())
                }

                when (versionHandling) {
                    Configuration.VersionHandling.ShowAllVersions -> true
                    Configuration.VersionHandling.ShowOldestVersionOnly -> conditions.add("is_newest_version = 1")
                    Configuration.VersionHandling.ShowNewestVersionOnly -> conditions.add(("is_oldest_version = 1"))
                }.apply {/* require handling all paths */}

                orderBy("time " + when (sorting) {
                    Configuration.Sorting.OldestFirst -> "ASC"
                    Configuration.Sorting.NewestFirst -> "DESC"
                })

                if (limit != null) {
                    limit(limit)
                }
            }
            .create()

    @Query("SELECT DISTINCT package FROM notifications ORDER BY package ASC")
    abstract fun getAppsWithNotifications(): LiveData<List<AppWithNotification>>

    @Query("DELETE FROM notifications WHERE package = :packageName")
    abstract fun deleteNotificationsByAppSync(packageName: String)

    @Query("DELETE FROM notifications WHERE time < :olderThanTimestamp")
    abstract fun deleteOldNotificationItemsSync(olderThanTimestamp: Long)
}
