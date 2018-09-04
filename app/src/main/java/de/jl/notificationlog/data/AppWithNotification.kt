package de.jl.notificationlog.data

import android.arch.persistence.room.ColumnInfo

data class AppWithNotification(
        @ColumnInfo(name = "package")
        val packageName: String
)