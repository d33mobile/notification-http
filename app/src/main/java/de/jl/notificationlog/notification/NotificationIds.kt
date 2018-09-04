package de.jl.notificationlog.notification

import android.content.Context
import android.os.Looper
import android.support.v4.app.NotificationCompat
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView

data class NotificationIds(
        val titleId: Int,
        val textId: Int
) {
    companion object {
        private val lock = Object()
        private var instance: NotificationIds? = null

        private const val CHANNEL = "CHANNEL"
        private const val TITLE = "TITLE"
        private const val TEXT = "TEXT"

        fun with(context: Context): NotificationIds {
            if (instance == null) {
                synchronized(lock) {
                    if (instance == null) {
                        try {
                            Looper.prepare()
                        } catch (ex: RuntimeException) {
                            // catches only one Looper per thread rule
                        }

                        val notification = NotificationCompat.Builder(context, CHANNEL)
                                .setContentTitle(TITLE)
                                .setContentText(TEXT)
                                .build()

                        val view = LayoutInflater.from(context).inflate(notification.contentView.layoutId, null) as ViewGroup
                        notification.contentView.reapply(context, view)

                        instance = detectNotificationViewGroupIds(view)
                    }
                }
            }

            return instance!!
        }

        private fun detectNotificationViewGroupIds(viewGroup: ViewGroup): NotificationIds {
            val result = NotificationIdsBuilder.create()

            detectNotificationViewGroupIds(viewGroup, result)

            return result.toNotificationIds()
        }

        private fun detectNotificationViewGroupIds(viewGroup: ViewGroup, notificationIds: NotificationIdsBuilder) {
            for (i in 0 until viewGroup.childCount) {
                val child = viewGroup.getChildAt(i)

                if (child is ViewGroup) {
                    detectNotificationViewGroupIds(child, notificationIds)
                } else if (child is TextView) {
                    when (child.text.toString()) {
                        TITLE -> notificationIds.titleId = child.id
                        TEXT -> notificationIds.textId = child.id
                    }
                }
            }
        }
    }
}

data class NotificationIdsBuilder(
        var titleId: Int?,
        var textId: Int?
) {
    companion object {
        fun create() = NotificationIdsBuilder(null, null)
    }

    fun toNotificationIds() = NotificationIds(
            titleId = titleId!!,
            textId = textId!!
    )
}