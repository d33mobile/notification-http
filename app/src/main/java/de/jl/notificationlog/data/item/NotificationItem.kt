package de.jl.notificationlog.data.item

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.annotation.NonNull
import androidx.room.Index

@Entity(
        tableName = "notifications",
        indices = [
            Index(
                    name = "notifications_index_time",
                    value = ["time"]
            ),
            Index(
                    name = "notifications_index_app_and_time",
                    value = ["package", "time"]
            ),
            Index(
                    name = "notifications_oldest_index_time",
                    value = ["is_oldest_version", "time"]
            ),
            Index(
                    name = "notifications_oldest_index_app_and_time",
                    value = ["is_oldest_version", "package", "time"]
            ),
            Index(
                    name = "notifications_newest_index_time",
                    value = ["is_newest_version", "time"]
            ),
            Index(
                    name = "notifications_newest_index_app_and_time",
                    value = ["is_newest_version", "package", "time"]
            ),
            Index(
                    name = "notifications_index_duplicate_group",
                    value = ["duplicate_group_id"]
            ),
            Index(
                    name = "notifications_index_app_duplicate_group",
                    value = ["package", "duplicate_group_id"]
            )
        ]
)
data class NotificationItem(
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = "id")
        val id: Long,
        @NonNull
        @ColumnInfo(name = "package")
        val packageName: String,
        @NonNull
        @ColumnInfo(name = "time")
        val time: Long,
        @NonNull
        @ColumnInfo(name = "title")
        val title: String,
        @NonNull
        @ColumnInfo(name = "text")
        val text: String,
        @ColumnInfo(name = "progress")
        val progress: Int,
        @ColumnInfo(name = "progress_max")
        val progressMax: Int,
        @ColumnInfo(name = "progress_indeterminate")
        val progressIndeterminate: Boolean,
        @ColumnInfo(name = "is_oldest_version")
        val isOldestVersion: Boolean,
        @ColumnInfo(name = "is_newest_version")
        val isNewestVersion: Boolean,
        @ColumnInfo(name = "duplicate_group_id")
        val duplicateGroupId: Long
)