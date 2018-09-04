package de.jl.notificationlog.data

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.support.annotation.NonNull

@Entity(tableName = "notifications")
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
        val text: String
)