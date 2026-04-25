package de.jl.notificationlog.util

import android.app.Application
import android.content.Context
import android.preference.PreferenceManager

class Configuration(context: Application) {
    companion object {
        private const val IS_WHITELIST_MODE = "whitelist_mode"
        private const val INVERTED_APPS_PACKAGE_NAMES = "inverted_app_package_names"
        private const val APP_SORTING = "app_sorting"
        private const val APP_SORTING_ALPHABETICALLY = "alphabetically"
        private const val APP_SORTING_NEWEST_FIRST = "newest_first"
        private const val APP_SORTING_OLDEST_FIRST = "oldest_first"
        private const val NOTIFICATION_SORTING = "sorting"
        private const val NOTIFICATION_SORT_NEWEST_FIRST = "newest_first"
        private const val NOTIFICATION_SORT_OLDEST_FIRST = "oldest_first"
        private const val VERSION_HANDLING = "version_handling"
        private const val VERSION_ALL = "all"
        private const val VERSION_OLDEST = "oldest"
        private const val VERSION_NEWEST = "newest"
        private const val OPEN_NOTIFICATIONS = "open_notifications"
        private const val NOTIFICATION_KEEPING_DAYS = "notification_keeping_days"
        private const val HIDE_DUPLICATES = "hide_duplicates"
        private const val REQUIRE_AUTH = "require_auth"
        private const val WEBHOOK_ENABLED = "webhook_enabled"
        private const val WEBHOOK_URL = "webhook_url"
        private const val WEBHOOK_BEARER_TOKEN = "webhook_bearer_token"
        private const val WEBHOOK_SKIP_ONGOING = "webhook_skip_ongoing"

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

    var appSorting: AppSorting
        get() = when(preferences.getString(APP_SORTING, APP_SORTING_ALPHABETICALLY)) {
            APP_SORTING_ALPHABETICALLY -> AppSorting.Alphabetically
            APP_SORTING_NEWEST_FIRST -> AppSorting.NewestFirst
            APP_SORTING_OLDEST_FIRST -> AppSorting.OldestFirst
            else -> throw IllegalArgumentException()
        }
        set(value) {
            preferences.edit()
                .putString(APP_SORTING, when (value) {
                    AppSorting.Alphabetically -> APP_SORTING_ALPHABETICALLY
                    AppSorting.NewestFirst -> APP_SORTING_NEWEST_FIRST
                    AppSorting.OldestFirst -> APP_SORTING_OLDEST_FIRST
                })
                .apply()
        }

    var notificationSorting: NotificationSorting
        get() = when (preferences.getString(NOTIFICATION_SORTING, NOTIFICATION_SORT_OLDEST_FIRST)) {
            NOTIFICATION_SORT_OLDEST_FIRST -> NotificationSorting.OldestFirst
            NOTIFICATION_SORT_NEWEST_FIRST -> NotificationSorting.NewestFirst
            else -> throw IllegalArgumentException()
        }
        set(value) {
            preferences.edit()
                    .putString(NOTIFICATION_SORTING, when (value) {
                        NotificationSorting.OldestFirst -> NOTIFICATION_SORT_OLDEST_FIRST
                        NotificationSorting.NewestFirst -> NOTIFICATION_SORT_NEWEST_FIRST
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

    var hideDuplicates: Boolean
        get() = preferences.getBoolean(HIDE_DUPLICATES, false)
        set(value) {
            preferences.edit()
                    .putBoolean(HIDE_DUPLICATES, value)
                    .apply()
        }

    var requireAuth: Boolean
        get() = preferences.getBoolean(REQUIRE_AUTH, false)
        set(value) {
            preferences.edit()
                    .putBoolean(REQUIRE_AUTH, value)
                    .apply()
    }

    var webhookEnabled: Boolean
        get() = preferences.getBoolean(WEBHOOK_ENABLED, false)
        set(value) = preferences.edit().putBoolean(WEBHOOK_ENABLED, value).apply()

    var webhookUrl: String
        get() = preferences.getString(WEBHOOK_URL, "") ?: ""
        set(value) = preferences.edit().putString(WEBHOOK_URL, value).apply()

    var webhookBearerToken: String
        get() = preferences.getString(WEBHOOK_BEARER_TOKEN, "") ?: ""
        set(value) = preferences.edit().putString(WEBHOOK_BEARER_TOKEN, value).apply()

    var webhookSkipOngoing: Boolean
        get() = preferences.getBoolean(WEBHOOK_SKIP_ONGOING, true)  // safe default: drop foreground-service spam
        set(value) = preferences.edit().putBoolean(WEBHOOK_SKIP_ONGOING, value).apply()

    enum class AppSorting {
        Alphabetically,
        NewestFirst,
        OldestFirst
    }

    enum class NotificationSorting {
        NewestFirst,
        OldestFirst
    }

    enum class VersionHandling {
        ShowAllVersions,
        ShowNewestVersionOnly,
        ShowOldestVersionOnly
    }
}
