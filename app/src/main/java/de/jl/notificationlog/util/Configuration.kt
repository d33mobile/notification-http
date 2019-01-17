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

    val versionHandling = VersionHandling.ShowAllVersions

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
