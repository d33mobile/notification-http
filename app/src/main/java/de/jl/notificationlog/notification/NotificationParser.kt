package de.jl.notificationlog.notification

import android.app.Notification
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import android.util.Log
import android.util.SparseArray
import android.widget.RemoteViews
import java.lang.reflect.Field
import java.util.*


object NotificationParser {
    private const val LOG_TAG = "NotificationParser"

    fun parse(notification: Notification, context: Context): NotificationData {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val extras = notification.extras
            val title = extras.getString(Notification.EXTRA_TITLE)?.toString()
            val text = extractRichText(extras)
            val progress = extras.getInt(Notification.EXTRA_PROGRESS)
            val progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX)
            val progressIndeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE)

            return NotificationData(
                    title = title ?: "",
                    text = text,
                    progress = progress,
                    progressMax = progressMax,
                    progressIndeterminate = progressIndeterminate
            )
        } else {
            return parseOld(notification, context)
        }
    }

    /**
     * EXTRA_TEXT alone is often a one-line summary ("(2 new messages)") while the actual
     * body sits in a style-specific extra. Pick the richest payload available, in this order:
     *
     *   1. MessagingStyle (Signal, Telegram, WhatsApp, …) — EXTRA_MESSAGES + EXTRA_HISTORIC_MESSAGES,
     *      flattened "sender: text" per line. This is what users actually want to see.
     *   2. BigTextStyle (Gmail, long bodies) — EXTRA_BIG_TEXT, the full body the OS shows when
     *      the notification is expanded.
     *   3. InboxStyle (grouped messages) — EXTRA_TEXT_LINES, joined with newlines.
     *   4. Plain EXTRA_TEXT — the upstream behaviour, kept as final fallback.
     */
    private fun extractRichText(extras: Bundle): String {
        extractMessages(extras)?.let { if (it.isNotBlank()) return it }
        extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                ?.takeIf { it.isNotBlank() }?.let { return it }
        extractTextLines(extras)?.let { if (it.isNotBlank()) return it }
        return extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
    }

    private fun extractMessages(extras: Bundle): String? {
        val combined = mutableListOf<Parcelable>()
        @Suppress("DEPRECATION") (extras.getParcelableArray(Notification.EXTRA_HISTORIC_MESSAGES))?.let { combined.addAll(it) }
        @Suppress("DEPRECATION") (extras.getParcelableArray(Notification.EXTRA_MESSAGES))?.let { combined.addAll(it) }
        if (combined.isEmpty()) return null
        val sb = StringBuilder()
        for (p in combined) {
            val b = p as? Bundle ?: continue
            // Bundle keys per Notification.MessagingStyle.Message.toBundle() in framework.
            val text = b.getCharSequence("text")?.toString() ?: continue
            val sender = b.getCharSequence("sender")?.toString().orEmpty()
            if (sb.isNotEmpty()) sb.append('\n')
            if (sender.isNotEmpty()) sb.append(sender).append(": ")
            sb.append(text)
        }
        return if (sb.isEmpty()) null else sb.toString()
    }

    private fun extractTextLines(extras: Bundle): String? {
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES) ?: return null
        if (lines.isEmpty()) return null
        return lines.joinToString("\n")
    }

    private fun parseOld(notification: Notification, context: Context): NotificationData {
        val notificationIdDetector = NotificationIds.with(context)
        val strings = getStringsFromRemoteViews(notification.contentView)

        val title = strings.get(notificationIdDetector.titleId, "")
        val text = strings.get(notificationIdDetector.textId, "")

        return NotificationData(
                title = title,
                text = text,
                progress = 0,
                progressIndeterminate = false,
                progressMax = 0
        )
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
