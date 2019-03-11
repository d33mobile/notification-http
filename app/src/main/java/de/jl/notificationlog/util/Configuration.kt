package de.jl.notificationlog.util

import android.app.Application
import android.content.Context
import android.preference.PreferenceManager

class Configuration(context: Application) {
    companion object {
        private const val IS_WHITELIST_MODE = "whitelist_mode"
        private const val INVERTED_APPS_PACKAGE_NAMES = "inverted_app_package_names"
        private const val SORTING = "sorting"
        private const val SORT_NEWEST_FIRST = "newest_first"
        private const val SORT_OLDEST_FIRST = "oldest_first"
        private const val VERSION_HANDLING = "version_handling"
        private const val VERSION_ALL = "all"
        private const val VERSION_OLDEST = "oldest"
        private const val VERSION_NEWEST = "newest"
        private const val OPEN_NOTIFICATIONS = "open_notifications"
        private const val NOTIFICATION_KEEPING_DAYS = "notification_keeping_days"

        private var instance: Configuration? = null
        private val lock = Object()

        fun with(context: Context): Configuration {
            if (instance == null) {
                synchronized(lock) {
                    if (instance == null) {
                        instance = Configuration(context.applicationContext as Application)
                    }
                }
            }

            return instance!!
        }
    }

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    var isWhitelistMode: Boolean
        get() = preferences.getBoolean(IS_WHITELIST_MODE, false)
        set(value) = preferences.edit()
                .putBoolean(IS_WHITELIST_MODE, value)
                .apply()

    val invertedApps: Set<String>
        get() = preferences.getStringSet(INVERTED_APPS_PACKAGE_NAMES, emptySet())!!

    fun shouldLogNotifications(appPackageName: String) = invertedApps.contains(appPackageName) == isWhitelistMode

    fun setShouldLogNotifications(appPackageName: String, shouldLog: Boolean) {
        if (shouldLog != shouldLogNotifications(appPackageName)) {
            toggleInvertedApp(appPackageName)
        }
    }

    fun toggleInvertedApp(appPackageName: String) {
        preferences.edit()
                .putStringSet(
                        INVERTED_APPS_PACKAGE_NAMES,
                        invertedApps.toMutableSet().apply {
                            if (contains(appPackageName)) {
                                remove(appPackageName)
                            } else {
                                add(appPackageName)
                            }
                        }
                )
                .apply()
    }

    var sorting: Sorting
        get() = when (preferences.getString(SORTING, SORT_OLDEST_FIRST)) {
            SORT_OLDEST_FIRST -> Sorting.OldestFirst
            SORT_NEWEST_FIRST -> Sorting.NewestFirst
            else -> throw IllegalArgumentException()
        }
        set(value) {
            preferences.edit()
                    .putString(SORTING, when (value) {
                        Sorting.OldestFirst -> SORT_OLDEST_FIRST
                        Sorting.NewestFirst -> SORT_NEWEST_FIRST
                    })
                    .apply()
        }

    var versionHandling: VersionHandling
        get() = when (preferences.getString(VERSION_HANDLING, VERSION_ALL)) {
            VERSION_ALL -> VersionHandling.ShowAllVersions
            VERSION_OLDEST -> VersionHandling.ShowOldestVersionOnly
            VERSION_NEWEST -> VersionHandling.ShowNewestVersionOnly
            else -> throw IllegalArgumentException()
        }
        set(value) {
            preferences.edit()
                    .putString(VERSION_HANDLING, when (value) {
                        VersionHandling.ShowAllVersions -> VERSION_ALL
                        VersionHandling.ShowOldestVersionOnly -> VERSION_OLDEST
                        VersionHandling.ShowNewestVersionOnly -> VERSION_NEWEST
                    })
                    .apply()
        }

    var openNotifications: Boolean
        get() = preferences.getBoolean(OPEN_NOTIFICATIONS, false)
        set(value) {
            preferences.edit()
                    .putBoolean(OPEN_NOTIFICATIONS, value)
                    .apply()
        }

    var notificationKeepingDays: Int
        get() = preferences.getInt(NOTIFICATION_KEEPING_DAYS, 0)
        set(value) {
            preferences.edit()
                    .putInt(NOTIFICATION_KEEPING_DAYS, value)
                    .apply()
        }

    enum class Sorting {
        NewestFirst,
        OldestFirst
    }

    enum class VersionHandling {
        ShowAllVersions,
        ShowNewestVersionOnly,
        ShowOldestVersionOnly
    }
}
