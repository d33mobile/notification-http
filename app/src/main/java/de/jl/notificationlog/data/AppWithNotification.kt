package de.jl.notificationlog.data

import androidx.room.ColumnInfo

data class AppWithNotification(
        @ColumnInfo(name = "package")
        val packageName: String
)
