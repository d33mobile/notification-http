package de.jl.notificationlog.data.item

import androidx.room.ColumnInfo

data class AppWithNotification(
        @ColumnInfo(name = "package")
        val packageName: String
)
