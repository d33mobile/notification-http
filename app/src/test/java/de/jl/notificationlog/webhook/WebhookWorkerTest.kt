package de.jl.notificationlog.webhook

import android.content.Context
import android.preference.PreferenceManager
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import de.jl.notificationlog.data.AppDatabase
import de.jl.notificationlog.data.item.NotificationItem
import de.jl.notificationlog.data.item.PendingWebhookDelivery
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Field
import java.util.concurrent.TimeUnit

/**
 * JVM tests for the webhook delivery path. Drives WebhookWorker directly via
 * TestListenableWorkerBuilder against a real in-memory Room DB and a MockWebServer.
 * No emulator required.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = android.app.Application::class)
class WebhookWorkerTest {

    private lateinit var server: MockWebServer
    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        server = MockWebServer().apply { start() }

        // Reset AppDatabase singleton between tests by reflection (the upstream class
        // exposes only `with(context)`; we want a fresh in-memory DB per test).
        resetAppDatabaseSingleton()
        injectInMemoryDatabase()

        // Reset prefs
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()

        // WorkManager init for any code path that touches it (we drive worker manually)
        WorkManagerTestInitHelper.initializeTestWorkManager(
                context,
                Configuration.Builder()
                        .setExecutor(SynchronousExecutor())
                        .setTaskExecutor(SynchronousExecutor())
                        .build()
        )
    }

    @After
    fun teardown() {
        server.shutdown()
        db.close()
        resetAppDatabaseSingleton()
    }

    private fun setSingleton(value: AppDatabase?) {
        // Kotlin lifts companion's private var to a static field on the outer class.
        val instanceField: Field = AppDatabase::class.java.getDeclaredField("instance")
                .apply { isAccessible = true }
        instanceField.set(null, value)
    }

    private fun resetAppDatabaseSingleton() = setSingleton(null)

    private fun injectInMemoryDatabase() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        setSingleton(db)
    }

    private fun enable(url: String, token: String = "") {
        de.jl.notificationlog.util.Configuration.with(context).apply {
            webhookEnabled = true
            webhookUrl = url
            webhookBearerToken = token
        }
    }

    private fun insertNotification(title: String, text: String, pkg: String = "com.example.app"): Long {
        val id = db.notification().insertSyncHandlePossibleDuplicate(
                packageName = pkg, time = System.currentTimeMillis(),
                title = title, text = text,
                progress = 0, progressMax = 0, progressIndeterminate = false,
                isOldestVersion = true, isNewestVersion = true
        )
        db.pendingWebhookDelivery().enqueueSync(id)
        return id
    }

    private fun runWorker(): ListenableWorker.Result =
            runBlocking { TestListenableWorkerBuilder<WebhookWorker>(context).build().doWork() }

    /** Decode the Title header back to plain text whether it was sent as RFC 2047 or verbatim. */
    private fun decodeTitle(value: String?): String? {
        if (value == null) return null
        val match = Regex("""^=\?utf-8\?B\?([A-Za-z0-9+/=]+)\?=$""").matchEntire(value) ?: return value
        return String(android.util.Base64.decode(match.groupValues[1], android.util.Base64.NO_WRAP),
                Charsets.UTF_8)
    }

    @Test
    fun `2xx success deletes pending row and sends headers + body`() {
        enable(server.url("/topic").toString(), token = "secret")
        insertNotification("hello", "world")
        server.enqueue(MockResponse().setResponseCode(200))

        val result = runWorker()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(0, db.pendingWebhookDelivery().countSync())
        val req: RecordedRequest = server.takeRequest(2, TimeUnit.SECONDS)!!
        assertEquals("POST", req.method)
        assertEquals("/topic", req.path)
        // Robolectric has no PackageManager entry for "com.example.app" so the label
        // resolution falls back to the package name itself, prepended to the title.
        assertEquals("com.example.app: hello", decodeTitle(req.getHeader("Title")))
        assertEquals("com.example.app", req.getHeader("Tags"))
        assertEquals("Bearer secret", req.getHeader("Authorization"))
        assertEquals("world", req.body.readUtf8())
    }

    @Test
    fun `5xx returns retry and keeps pending row`() {
        enable(server.url("/t").toString())
        insertNotification("a", "b")
        server.enqueue(MockResponse().setResponseCode(500))

        val result = runWorker()

        assertEquals(ListenableWorker.Result.retry(), result)
        assertEquals(1, db.pendingWebhookDelivery().countSync())
    }

    @Test
    fun `408 Request Timeout retries (transient)`() {
        enable(server.url("/t").toString())
        insertNotification("a", "b")
        server.enqueue(MockResponse().setResponseCode(408))

        val result = runWorker()

        assertEquals(ListenableWorker.Result.retry(), result)
        assertEquals(1, db.pendingWebhookDelivery().countSync())
    }

    @Test
    fun `429 Too Many Requests retries (transient)`() {
        enable(server.url("/t").toString())
        insertNotification("a", "b")
        server.enqueue(MockResponse().setResponseCode(429))

        val result = runWorker()

        assertEquals(ListenableWorker.Result.retry(), result)
        assertEquals(1, db.pendingWebhookDelivery().countSync())
    }

    @Test
    fun `4xx drops pending row to avoid permanent stall`() {
        enable(server.url("/t").toString())
        insertNotification("a", "b")
        server.enqueue(MockResponse().setResponseCode(403))

        val result = runWorker()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(0, db.pendingWebhookDelivery().countSync())
    }

    @Test
    fun `bearer token absent when blank`() {
        enable(server.url("/t").toString(), token = "")
        insertNotification("a", "b")
        server.enqueue(MockResponse().setResponseCode(200))

        runWorker()
        val req = server.takeRequest(2, TimeUnit.SECONDS)!!
        assertNull(req.getHeader("Authorization"))
    }

    @Test
    fun `FIFO order across multiple pending notifications`() {
        enable(server.url("/t").toString())
        insertNotification("first", "1")
        insertNotification("second", "2")
        insertNotification("third", "3")
        repeat(3) { server.enqueue(MockResponse().setResponseCode(200)) }

        runWorker()

        val titles = (1..3).map { decodeTitle(server.takeRequest(2, TimeUnit.SECONDS)!!.getHeader("Title")) }
        assertEquals(listOf("com.example.app: first", "com.example.app: second", "com.example.app: third"), titles)
        assertEquals(0, db.pendingWebhookDelivery().countSync())
    }

    @Test
    fun `partial drain on 5xx — first delivered, second retried`() {
        enable(server.url("/t").toString())
        insertNotification("ok", "1")
        insertNotification("fail", "2")
        server.enqueue(MockResponse().setResponseCode(200))
        server.enqueue(MockResponse().setResponseCode(500))

        val result = runWorker()

        assertEquals(ListenableWorker.Result.retry(), result)
        assertEquals(1, db.pendingWebhookDelivery().countSync())
        // Next time worker runs (network returned), the second one should still be peekable.
        val pending = db.pendingWebhookDelivery().peekOldestNotificationSync()
        assertNotNull(pending)
        assertEquals("fail", pending!!.title)
    }

    @Test
    fun `recovery after restart — second worker run drains rows left by first`() {
        enable(server.url("/t").toString())
        insertNotification("a", "1")
        insertNotification("b", "2")
        // First run: server is offline / errors out
        server.enqueue(MockResponse().setResponseCode(500))
        runWorker()
        assertEquals(2, db.pendingWebhookDelivery().countSync())

        // "Restart" — fresh worker instance, server back up
        server.enqueue(MockResponse().setResponseCode(200))
        server.enqueue(MockResponse().setResponseCode(200))
        val result = runWorker()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(0, db.pendingWebhookDelivery().countSync())
    }

    @Test
    fun `disabled at runtime drains and drops queue`() {
        // Pre-populate while enabled, then disable
        enable(server.url("/t").toString())
        insertNotification("a", "1")
        insertNotification("b", "2")
        de.jl.notificationlog.util.Configuration.with(context).webhookEnabled = false

        val result = runWorker()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(0, db.pendingWebhookDelivery().countSync())
        // No HTTP requests issued
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `non-ASCII title is RFC 2047 base64 encoded`() {
        enable(server.url("/t").toString())
        // "działa" — Polish ł + ó-style chars
        insertNotification(title = "działa żółć", text = "ascii body")
        server.enqueue(MockResponse().setResponseCode(200))

        runWorker()
        val req = server.takeRequest(2, TimeUnit.SECONDS)!!
        // RFC 2047 encoded-word: =?utf-8?B?<base64 of UTF-8 bytes>?=  (label-prefixed)
        assertEquals("com.example.app: działa żółć", decodeTitle(req.getHeader("Title")))
        assertEquals("ascii body", req.body.readUtf8())
    }

    @Test
    fun `pure ASCII title passes through verbatim (no encoded-word wrapper)`() {
        enable(server.url("/t").toString())
        insertNotification(title = "Plain title", text = "x")
        server.enqueue(MockResponse().setResponseCode(200))

        runWorker()
        val req = server.takeRequest(2, TimeUnit.SECONDS)!!
        assertEquals("com.example.app: Plain title", req.getHeader("Title"))
    }

    @Test
    fun `CR LF in title is stripped to spaces`() {
        enable(server.url("/t").toString())
        insertNotification(title = "line1\r\nline2", text = "x")
        server.enqueue(MockResponse().setResponseCode(200))

        runWorker()
        val req = server.takeRequest(2, TimeUnit.SECONDS)!!
        assertEquals("com.example.app: line1  line2", req.getHeader("Title"))
    }

    @Test
    fun `blank notification title uses just the app label`() {
        enable(server.url("/t").toString())
        insertNotification(title = "", text = "body only")
        server.enqueue(MockResponse().setResponseCode(200))

        runWorker()
        val req = server.takeRequest(2, TimeUnit.SECONDS)!!
        assertEquals("com.example.app", decodeTitle(req.getHeader("Title")))
        assertEquals("body only", req.body.readUtf8())
    }

    @Test
    fun `blank URL is no-op success`() {
        de.jl.notificationlog.util.Configuration.with(context).apply {
            webhookEnabled = true
            webhookUrl = ""
        }
        insertNotification("a", "1")

        val result = runWorker()

        assertEquals(ListenableWorker.Result.success(), result)
        // Pending row stays (because the worker doesn't drain when URL is blank — user
        // might be mid-edit; we don't want to lose data while they configure it)
        assertEquals(1, db.pendingWebhookDelivery().countSync())
    }
}
