package de.jl.notificationlog.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import de.jl.notificationlog.Application
import de.jl.notificationlog.util.Configuration

open class CheckAuthActivity: AppCompatActivity() {
    companion object {
        private const val REQUEST_AUTH = 123
    }

    val configuration: Configuration by lazy { Configuration.with(this) }
    val authUtil: CheckAuthUtil by lazy { (application as Application).checkAuthUtil }

    override fun onResume() {
        super.onResume()

        authUtil.reportResume()

        if (configuration.requireAuth && !authUtil.isAuthenticated) {
            authUtil.createIntent()?.let { intent ->
                startActivityForResult(intent, REQUEST_AUTH)
            }
        }
    }

    override fun onPause() {
        super.onPause()

        authUtil.reportPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_AUTH) {
            if (resultCode == RESULT_OK) {
                authUtil.isAuthenticated = true
            } else {
                finish()
            }
        }
    }
}