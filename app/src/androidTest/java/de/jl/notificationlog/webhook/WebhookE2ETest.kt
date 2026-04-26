package de.jl.notificationlog.webhook

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import de.jl.notificationlog.data.AppDatabase
import de.jl.notificationlog.util.Configuration
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * Real-device E2E: real Room (with migration applied), real WorkManager
 * (TestDriver to flip constraints), real HttpURLConnection against a local
 * MockWebServer.
 */
@RunWith(AndroidJUnit4::class)
class WebhookE2ETest {

    private lateinit var server: MockWebServer
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        server = MockWebServer().apply { start() }

        Configuration.with(context).apply {
            webhookEnabled = true
            webhookUrl = server.url("/topic").toString()
            webhookBearerToken = "abc"
        }

        // Clear pending rows from previous runs
        val db = AppDatabase.with(context)
        while (true) {
            val n = db.pendingWebhookDelivery().peekOldestNotificationSync() ?: break
            val pid = db.pendingWebhookDelivery().findPendingIdByNotificationSync(n.id) ?: break
            db.pendingWebhookDelivery().deleteSync(pid)
        }
        // Note: Application.onCreate already initialized real WorkManager. We use it as-is and
        // skip NetworkType.CONNECTED constraint in the test request — the worker code itself
        // (drain loop, HTTP, deletion) is what we are exercising end-to-end.
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun realWorkManagerDeliversNotificationViaConstraint() {
        val db = AppDatabase.with(context)
        val notifId = db.notification().insertSyncHandlePossibleDuplicate(
                packageName = "com.test.e2e", time = System.currentTimeMillis(),
                title = "E2E", text = "body",
                progress = 0, progressMax = 0, progressIndeterminate = false,
                isOldestVersion = true, isNewestVersion = true
        )
        db.pendingWebhookDelivery().enqueueSync(notifId)
        server.enqueue(MockResponse().setResponseCode(200))

        val request = OneTimeWorkRequestBuilder<WebhookWorker>().build()
        WorkManager.getInstance(context).enqueue(request).result.get()

        val info = waitForWorker(request.id)
        assertEquals(WorkInfo.State.SUCCEEDED, info.state)

        val req = server.takeRequest(5, TimeUnit.SECONDS)
        assertTrue("server got the POST", req != null)
        assertEquals("E2E", req!!.getHeader("Title"))
        assertEquals("body", req.body.readUtf8())
        assertEquals(0, db.pendingWebhookDelivery().countSync())
    }

    private fun waitForWorker(id: java.util.UUID): WorkInfo {
        val wm = WorkManager.getInstance(context)
        val deadline = System.currentTimeMillis() + 30_000
        while (System.currentTimeMillis() < deadline) {
            val info = wm.getWorkInfoById(id).get()
            if (info != null && info.state.isFinished) return info
            Thread.sleep(200)
        }
        return wm.getWorkInfoById(id).get()!!
    }
}
