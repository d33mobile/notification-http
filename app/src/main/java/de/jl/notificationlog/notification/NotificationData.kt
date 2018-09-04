package de.jl.notificationlog.notification

import android.text.TextUtils

class NotificationData(val title: String, val text: String) {
    val isEmpty: Boolean by lazy {
        TextUtils.isEmpty(title) && TextUtils.isEmpty(text)
    }
}
