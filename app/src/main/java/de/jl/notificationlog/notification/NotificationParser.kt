package de.jl.notificationlog.notification

import android.app.Notification
import android.content.Context
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import android.util.Log
import android.util.SparseArray
import android.widget.RemoteViews
import java.lang.reflect.Field
import java.util.*


object NotificationParser {
    private val LOG_TAG = "NotificationParser"

    fun parse(notification: Notification, context: Context): NotificationData {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val extras = notification.extras
            val title = extras.getString("android.title")!!.toString()
            val text = extras.getCharSequence("android.text")!!.toString()

            return NotificationData(
                    title = title,
                    text = text
            )
        } else {
            return parseOld(notification, context)
        }
    }

    private fun parseOld(notification: Notification, context: Context): NotificationData {
        val notificationIdDetector = NotificationIds.with(context)
        val strings = getStringsFromRemoteViews(notification.contentView)

        val title = strings.get(notificationIdDetector.titleId, "")
        val text = strings.get(notificationIdDetector.textId, "")

        return NotificationData(title, text)
    }

    // key = id; value = text
    private fun getStringsFromRemoteViews(view: RemoteViews): SparseArray<String> {
        val result = SparseArray<String>()

        try {
            val mActions: Field? = RemoteViews::class.java.getDeclaredField("mActions")
            if (mActions != null) {
                mActions.isAccessible = true

                val actions = mActions.get(view) as ArrayList<Parcelable>?
                if (actions != null) {
                    for (parcelable in actions) {
                        val parcel = Parcel.obtain()

                        parcelable.writeToParcel(parcel, 0)
                        parcel.setDataPosition(0)

                        // 2 = ReflectionAction
                        if (parcel.readInt() != 2) {
                            continue
                        }

                        val viewId = parcel.readInt()
                        val methodName = parcel.readString()

                        if (methodName == null) {
                            continue
                        } else if (methodName == "setText") {
                            // 10 means CharSequence
                            if (parcel.readInt() != 10)
                                continue
                            // get the string
                            val charSequence = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel)
                            if (charSequence != null)
                                result.put(viewId, charSequence.toString())
                        }

                        parcel.recycle()
                    }
                }
            }
        } catch (ex: IllegalAccessException) {
            Log.d(LOG_TAG, "getStringsFromRemoteViews", ex)
        } catch (ex: ClassCastException) {
            Log.d(LOG_TAG, "getStringsFromRemoteViews", ex)
        } catch (ex: NoSuchFieldException) {
            Log.d(LOG_TAG, "getStringsFromRemoteViews", ex)
        }

        return result
    }
}
