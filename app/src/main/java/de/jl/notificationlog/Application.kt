package de.jl.notificationlog

import android.app.Application
import de.jl.notificationlog.util.DeleteOldNotificationsUtil

class Application: Application() {
    override fun onCreate() {
        super.onCreate()

        // init the background job
        DeleteOldNotificationsUtil.with(this)
    }
}