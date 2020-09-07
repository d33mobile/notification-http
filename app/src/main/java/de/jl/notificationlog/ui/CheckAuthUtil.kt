package de.jl.notificationlog.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import de.jl.notificationlog.R

class CheckAuthUtil(private val context: Context) {
    companion object {
        val SUPPORTED = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
    }

    private val handler = Handler(Looper.getMainLooper())
    private val keyguardManager: KeyguardManager by lazy {
        context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    }

    private var counter = 0

    var isAuthenticated = false

    private val resetRunnable = Runnable { isAuthenticated = false }

    fun createIntent(): Intent {
        if (!SUPPORTED) {
            throw IllegalArgumentException()
        }

        return keyguardManager.createConfirmDeviceCredentialIntent(
                context.getString(R.string.app_name),
                context.getString(R.string.auth_message)
        )
    }

    fun reportResume() {
        counter++

        handler.removeCallbacks(resetRunnable)
    }

    fun reportPause() {
        counter--

        if (counter == 0) {
            handler.postDelayed(resetRunnable, 500)
        }

        if (counter < 0) {
            throw IllegalArgumentException()
        }
    }
}