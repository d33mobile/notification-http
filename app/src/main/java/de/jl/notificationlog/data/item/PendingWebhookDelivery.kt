package de.jl.notificationlog.data.item

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
        tableName = "pending_webhook_deliveries",
        indices = [
            Index(name = "pending_webhook_deliveries_index_notification_id", value = ["notification_id"])
        ],
        foreignKeys = [
            ForeignKey(
                    entity = NotificationItem::class,
                    parentColumns = ["id"],
                    childColumns = ["notification_id"],
                    onDelete = ForeignKey.CASCADE,
                    onUpdate = ForeignKey.CASCADE
            )
        ]
)
data class PendingWebhookDelivery(
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = "id")
        val id: Long,
        @ColumnInfo(name = "notification_id")
        val notificationId: Long
)
