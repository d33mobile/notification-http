package de.jl.notificationlog.webhook

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import de.jl.notificationlog.data.AppDatabase
import de.jl.notificationlog.data.item.NotificationItem
import de.jl.notificationlog.ui.AppsUtil
import de.jl.notificationlog.util.Configuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import android.util.Base64

class WebhookWorker(
        context: Context,
        params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val config = Configuration.with(applicationContext)
        if (!config.webhookEnabled) {
            // disabled at runtime — drop pending so they don't pile up forever
            AppDatabase.with(applicationContext).pendingWebhookDelivery().let { dao ->
                while (true) {
                    val n = dao.peekOldestNotificationSync() ?: break
                    val pid = dao.findPendingIdByNotificationSync(n.id) ?: break
                    dao.deleteSync(pid)
                }
            }
            return@withContext Result.success()
        }

        val url = config.webhookUrl
        if (url.isBlank()) {
            return@withContext Result.success()
        }
        val parsedUrl = try {
            URL(url)
        } catch (e: Exception) {
            Log.w(TAG, "Invalid webhook URL: $url", e)
            return@withContext Result.success()
        }

        val token = config.webhookBearerToken
        val dao = AppDatabase.with(applicationContext).pendingWebhookDelivery()

        while (true) {
            val notification = dao.peekOldestNotificationSync() ?: break
            val pendingId = dao.findPendingIdByNotificationSync(notification.id) ?: break

            when (val outcome = post(parsedUrl, token, notification)) {
                Outcome.Success, Outcome.ClientError -> dao.deleteSync(pendingId)
                Outcome.Retry -> {
                    Log.d(TAG, "Retrying webhook delivery later: ${outcome}")
                    return@withContext Result.retry()
                }
            }
        }

        Result.success()
    }

    private enum class Outcome { Success, ClientError, Retry }

    private fun post(url: URL, bearerToken: String, item: NotificationItem): Outcome {
        var conn: HttpURLConnection? = null
        return try {
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("Content-Type", "text/plain; charset=utf-8")
                setRequestProperty("Title", encodeHeader(buildTitle(item)))
                setRequestProperty("Tags", encodeHeader(item.packageName))
                if (bearerToken.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer $bearerToken")
                }
            }
            conn.outputStream.use { it.write(item.text.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            when {
                code in 200..299 -> Outcome.Success
                code in 400..499 -> {
                    Log.w(TAG, "Webhook 4xx (dropping notification ${item.id}): $code")
                    Outcome.ClientError
                }
                else -> {
                    Log.w(TAG, "Webhook ${code} for notification ${item.id} — will retry")
                    Outcome.Retry
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "Webhook IO error — will retry", e)
            Outcome.Retry
        } catch (e: Exception) {
            // unexpected: don't loop forever, drop
            Log.e(TAG, "Webhook unexpected error (dropping notification ${item.id})", e)
            Outcome.ClientError
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Compose the Title sent to the webhook: `<app-label>: <notification-title>`. The app label
     * (resolved via PackageManager) is prepended so subscribers know which app the notification
     * came from — `Tags` already carries the package name for automation/grepping. Falls back
     * to bare title when the notification's title is itself the label, and to packageName when
     * the app is no longer installed.
     */
    private fun buildTitle(item: NotificationItem): String {
        val label = AppsUtil.getAppTitle(item.packageName, applicationContext)
        val title = item.title.trim()
        return when {
            title.isBlank() -> label
            title == label -> label
            else -> "$label: $title"
        }
    }

    /**
     * HTTP headers must be ASCII (RFC 7230). For values with non-ASCII characters we use
     * RFC 2047 encoded-word `=?utf-8?B?<base64>?=`, which ntfy.sh decodes back to UTF-8.
     * Pure-ASCII values pass through verbatim (after CR/LF strip) to keep the wire format
     * readable in the common case.
     */
    private fun encodeHeader(value: String): String {
        val cleaned = value.replace('\r', ' ').replace('\n', ' ')
        if (cleaned.isEmpty()) return "-"
        val needsEncoding = cleaned.any { it.code !in 0x20..0x7E }
        if (!needsEncoding) return cleaned
        val b64 = Base64.encodeToString(cleaned.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        return "=?utf-8?B?$b64?="
    }

    companion object {
        private const val TAG = "WebhookWorker"
    }
}
