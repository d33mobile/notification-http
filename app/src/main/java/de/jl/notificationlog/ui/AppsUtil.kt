package de.jl.notificationlog.ui

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import de.jl.notificationlog.R

object AppsUtil {
    fun getAppTitle(packageName: String, context: Context): String {
        try {
            return context.packageManager.getApplicationInfo(packageName, 0)
                    .loadLabel(context.packageManager)
                    .toString()
        } catch (ex: PackageManager.NameNotFoundException) {
            return packageName
        }
    }

    fun getAppIcon(packageName: String, context: Context): Drawable? {
        try {
            return context.packageManager.getApplicationInfo(packageName, 0)
                    .loadIcon(context.packageManager)
        } catch (ex: PackageManager.NameNotFoundException) {
            return ContextCompat.getDrawable(context, R.mipmap.ic_app_removed)
        }
    }

    fun getAllApps(context: Context): List<App> = context.packageManager.getInstalledApplications(0).map {
        App(
                packageName = it.packageName,
                title = it.loadLabel(context.packageManager).toString()
        )
    }
}

data class App(
        val packageName: String,
        val title: String
)
