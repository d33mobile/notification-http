package de.jl.notificationlog.data.dao

import androidx.room.Dao
import androidx.room.Query
import de.jl.notificationlog.data.item.NotificationItem

@Dao
abstract class PendingWebhookDeliveryDao {
    @Query("INSERT INTO pending_webhook_deliveries (notification_id) VALUES (:notificationId)")
    abstract fun enqueueSync(notificationId: Long): Long

    @Query("""
        SELECT n.* FROM pending_webhook_deliveries p
        JOIN notifications n ON n.id = p.notification_id
        ORDER BY p.id ASC
        LIMIT 1
    """)
    abstract fun peekOldestNotificationSync(): NotificationItem?

    @Query("""
        SELECT id FROM pending_webhook_deliveries
        WHERE notification_id = :notificationId
        ORDER BY id ASC
        LIMIT 1
    """)
    abstract fun findPendingIdByNotificationSync(notificationId: Long): Long?

    @Query("DELETE FROM pending_webhook_deliveries WHERE id = :pendingId")
    abstract fun deleteSync(pendingId: Long)

    @Query("SELECT COUNT(*) FROM pending_webhook_deliveries")
    abstract fun countSync(): Int
}
