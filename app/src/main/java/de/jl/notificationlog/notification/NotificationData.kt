package de.jl.notificationlog.notification

import android.text.TextUtils

class NotificationData(
        val title: String,
        val text: String,
        val progress: Int,
        val progressMax: Int,
        val progressIndeterminate: Boolean
) {
    val isEmpty: Boolean by lazy {
        TextUtils.isEmpty(title) && TextUtils.isEmpty(text)
    }
}
