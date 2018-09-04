package de.jl.notificationlog.ui

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
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
}