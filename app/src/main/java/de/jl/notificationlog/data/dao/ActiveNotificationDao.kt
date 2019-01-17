package de.jl.notificationlog.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import de.jl.notificationlog.data.item.ActiveNotificationItem

@Dao
interface ActiveNotificationDao {
    @Query("SELECT * FROM active_notifications WHERE app_package_name = :appPackageName AND system_id = :systemId AND system_tag = :systemTag")
    fun querySync(
            appPackageName: String,
            systemId: Int,
            systemTag: String?
    ): ActiveNotificationItem?

    @Insert
    fun insertSync(item: ActiveNotificationItem)

    @Query("UPDATE active_notifications SET previous_notification_item_id = :lastNotificationId WHERE id = :activeNotificationId")
    fun updateLastNotificationIdSync(
            activeNotificationId: Long,
            lastNotificationId: Long
    )

    @Query("DELETE FROM active_notifications WHERE app_package_name = :appPackageName AND system_id = :systemId AND system_tag = :systemTag")
    fun removeSync(
            appPackageName: String,
            systemId: Int,
            systemTag: String?
    )
}