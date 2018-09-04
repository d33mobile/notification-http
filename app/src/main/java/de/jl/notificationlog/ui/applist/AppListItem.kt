package de.jl.notificationlog.ui.applist

sealed class AppListItem {
    abstract fun getItemId(): Long
}

object MissingPermissionAppListItem: AppListItem() {
    override fun getItemId() = Long.MAX_VALUE - 1
}

object NoDataAppListItem: AppListItem() {
    override fun getItemId() = Long.MAX_VALUE - 2
}

object AllAppsListItem: AppListItem() {
    override fun getItemId() = Long.MAX_VALUE - 3
}

data class AppEntryAppListItem(val title: String, val packageName: String): AppListItem() {
    override fun getItemId() = packageName.hashCode().toLong()
}