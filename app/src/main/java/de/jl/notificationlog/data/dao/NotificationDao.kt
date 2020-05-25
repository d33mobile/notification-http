package de.jl.notificationlog.data.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import de.jl.notificationlog.data.item.AppWithNotification
import de.jl.notificationlog.data.item.NotificationItem
import de.jl.notificationlog.util.Configuration

@Dao
abstract class NotificationDao {
    @Transaction
    open fun insertSyncHandlePossibleDuplicate(
            packageName: String,
            time: Long,
            title: String,
            text: String,
            progress: Int,
            progressMax: Int,
            progressIndeterminate: Boolean,
            isOldestVersion: Boolean,
            isNewestVersion: Boolean
    ): Long {
        val previous = getLastNotificationByAppSync(packageName)

        if (
            previous != null &&
            previous.title == title && previous.text == text &&
            previous.progress == progress && previous.progressMax == progressMax &&
            previous.progressIndeterminate == progressIndeterminate
        ) {
            return insertSync(
                    packageName = packageName,
                    time = time,
                    title = title,
                    text = text,
                    progress = progress,
                    progressMax = progressMax,
                    progressIndeterminate = progressIndeterminate,
                    isOldestVersion = isOldestVersion,
                    isNewestVersion = isNewestVersion,
                    duplicateGroupdId = previous.duplicateGroupId
            )
        } else {
            val notificationId = insertSync(
                    packageName = packageName,
                    time = time,
                    title = title,
                    text = text,
                    progress = progress,
                    progressMax = progressMax,
                    progressIndeterminate = progressIndeterminate,
                    isOldestVersion = isOldestVersion,
                    isNewestVersion = isNewestVersion,
                    duplicateGroupdId = 0
            )

            setDuplicateGroupId(
                    notificationId = notificationId,
                    duplicateGroupdId = notificationId
            )

            return notificationId
        }
    }

    @Query("INSERT INTO notifications (package, time, title, text, progress, progress_max, progress_indeterminate, is_oldest_version, is_newest_version, duplicate_group_id) VALUES (:packageName, :time, :title, :text, :progress, :progressMax, :progressIndeterminate, :isOldestVersion, :isNewestVersion, :duplicateGroupdId)")
    protected abstract fun insertSync(
            packageName: String,
            time: Long,
            title: String,
            text: String,
            progress: Int,
            progressMax: Int,
            progressIndeterminate: Boolean,
            isOldestVersion: Boolean,
            isNewestVersion: Boolean,
            duplicateGroupdId: Long
    ): Long

    @Query("SELECT * FROM notifications WHERE package = :packageName ORDER BY time DESC LIMIT 1")
    protected abstract fun getLastNotificationByAppSync(packageName: String): NotificationItem?

    @Query("UPDATE notifications SET duplicate_group_id = :duplicateGroupdId WHERE id = :notificationId")
    protected abstract fun setDuplicateGroupId(notificationId: Long, duplicateGroupdId: Long)

    @Query("UPDATE NOTIFICATIONS SET is_newest_version = :isNewestNotification WHERE id = :id")
    abstract fun setIsNewestNotificationSync(id: Long, isNewestNotification: Boolean)

    @RawQuery(observedEntities = [ NotificationItem::class ])
    protected abstract fun getNotificationsLive(query: SupportSQLiteQuery): DataSource.Factory<Int, NotificationItem>

    @RawQuery(observedEntities = [ NotificationItem::class ])
    protected abstract fun getNotificationsSync(query: SupportSQLiteQuery): List<NotificationItem>

    fun getNotifications(
            packageName: String?, sorting: Configuration.Sorting,
            versionHandling: Configuration.VersionHandling, hideDuplicates: Boolean
    ) = getNotificationsLive(
            buildSelectQuery(
                    packageName = packageName,
                    sorting = sorting,
                    versionHandling = versionHandling,
                    limit = null,
                    hideDuplicates = hideDuplicates
            )
    )

    fun getNotificationsPageSync(
            packageName: String?, rows: Int, offset: Int, sorting: Configuration.Sorting,
            versionHandling: Configuration.VersionHandling, hideDuplicates: Boolean
    ) = getNotificationsSync(
            buildSelectQuery(
                    packageName = packageName,
                    sorting = sorting,
                    versionHandling = versionHandling,
                    limit = "$offset,$rows",
                    hideDuplicates = hideDuplicates
            )
    )

    fun buildSelectQuery(
            packageName: String?, sorting: Configuration.Sorting, versionHandling: Configuration.VersionHandling,
            limit: String?, hideDuplicates: Boolean
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

                if (hideDuplicates) {
                    columns(arrayOf(
                            "MIN(id) AS id", "package",
                            "MIN(time) AS time",
                            "title", "text", "progress", "progress_max", "progress_indeterminate",
                            // these values are not used in the UI
                            // "oldest_version", "is_newest_version"
                            "duplicate_group_id"
                    ))

                    groupBy("duplicate_group_id")
                }

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
