package de.jl.notificationlog.notification

import android.app.Notification
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = android.app.Application::class)
class NotificationParserTest {
    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun `plain EXTRA_TEXT is returned verbatim (upstream behaviour)`() {
        val n = Notification().apply {
            extras = Bundle().apply {
                putString(Notification.EXTRA_TITLE, "App")
                putCharSequence(Notification.EXTRA_TEXT, "hi")
            }
        }
        val r = NotificationParser.parse(n, ctx)
        assertEquals("App", r.title)
        assertEquals("hi", r.text)
    }

    @Test
    fun `EXTRA_BIG_TEXT wins over EXTRA_TEXT (long bodies, gmail-style)`() {
        val n = Notification().apply {
            extras = Bundle().apply {
                putString(Notification.EXTRA_TITLE, "Mail")
                putCharSequence(Notification.EXTRA_TEXT, "summary line")
                putCharSequence(Notification.EXTRA_BIG_TEXT, "Long\nmulti-line\nbody")
            }
        }
        val r = NotificationParser.parse(n, ctx)
        assertEquals("Long\nmulti-line\nbody", r.text)
    }

    @Test
    fun `EXTRA_TEXT_LINES wins over EXTRA_TEXT (inbox-style grouped msgs)`() {
        val n = Notification().apply {
            extras = Bundle().apply {
                putCharSequence(Notification.EXTRA_TEXT, "(3 new)")
                putCharSequenceArray(Notification.EXTRA_TEXT_LINES,
                        arrayOf<CharSequence>("line A", "line B", "line C"))
            }
        }
        assertEquals("line A\nline B\nline C", NotificationParser.parse(n, ctx).text)
    }

    @Test
    fun `MessagingStyle EXTRA_MESSAGES wins (Signal Telegram WhatsApp)`() {
        val msg1 = Bundle().apply {
            putCharSequence("text", "hello there")
            putCharSequence("sender", "Alice")
        }
        val msg2 = Bundle().apply {
            putCharSequence("text", "general kenobi")
            putCharSequence("sender", "Bob")
        }
        val n = Notification().apply {
            extras = Bundle().apply {
                putString(Notification.EXTRA_TITLE, "Group chat")
                putCharSequence(Notification.EXTRA_TEXT, "(2 new)")
                putCharSequence(Notification.EXTRA_BIG_TEXT, "should be ignored")
                putParcelableArray(Notification.EXTRA_MESSAGES, arrayOf<android.os.Parcelable>(msg1, msg2))
            }
        }
        assertEquals("Alice: hello there\nBob: general kenobi",
                NotificationParser.parse(n, ctx).text)
    }

    @Test
    fun `historic messages prepended to current messages`() {
        val historic = Bundle().apply {
            putCharSequence("text", "earlier message")
            putCharSequence("sender", "Alice")
        }
        val current = Bundle().apply {
            putCharSequence("text", "newer message")
            putCharSequence("sender", "Alice")
        }
        val n = Notification().apply {
            extras = Bundle().apply {
                putParcelableArray(Notification.EXTRA_HISTORIC_MESSAGES,
                        arrayOf<android.os.Parcelable>(historic))
                putParcelableArray(Notification.EXTRA_MESSAGES,
                        arrayOf<android.os.Parcelable>(current))
            }
        }
        assertEquals("Alice: earlier message\nAlice: newer message",
                NotificationParser.parse(n, ctx).text)
    }

    @Test
    fun `MessagingStyle without sender (you-message) renders bare text`() {
        val msg = Bundle().apply { putCharSequence("text", "self note") }
        val n = Notification().apply {
            extras = Bundle().apply {
                putParcelableArray(Notification.EXTRA_MESSAGES, arrayOf<android.os.Parcelable>(msg))
            }
        }
        assertEquals("self note", NotificationParser.parse(n, ctx).text)
    }

    @Test
    fun `falls back to empty string when no body extras present`() {
        val n = Notification().apply { extras = Bundle().apply { putString(Notification.EXTRA_TITLE, "T") } }
        assertEquals("", NotificationParser.parse(n, ctx).text)
    }
}
