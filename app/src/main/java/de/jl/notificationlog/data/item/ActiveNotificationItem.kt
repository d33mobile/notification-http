package de.jl.notificationlog.data.item

import androidx.room.*

@Entity(
        tableName = "active_notifications",
        indices = [
            Index(
                    name = "active_notifications_query_index",
                    value = ["app_package_name", "system_id", "system_tag"]
            ),
            Index(
                    name = "active_notification_previous_notification_item_index",
                    value = ["previous_notification_item_id"]
            )
        ],
        foreignKeys = [
            ForeignKey(
                    entity = NotificationItem::class,
                    childColumns = [
                        "previous_notification_item_id"
                    ],
                    parentColumns = [
                        "id"
                    ],
                    onUpdate = ForeignKey.CASCADE,
                    onDelete = ForeignKey.CASCADE
            )
        ]
)
data class ActiveNotificationItem(
        @ColumnInfo(name = "id")
        @PrimaryKey(autoGenerate = true)
        val id: Long,
        @ColumnInfo(name = "app_package_name")
        val appPackageName: String,
        @ColumnInfo(name = "system_id")
        val systemId: Int,
        @ColumnInfo(name = "system_tag")
        val systemTag: String,
        @ColumnInfo(name = "previous_notification_item_id")
        val previousNotificationItemId: Long
)