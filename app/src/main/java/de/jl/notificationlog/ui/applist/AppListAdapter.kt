package de.jl.notificationlog.ui.applist

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.jl.notificationlog.R
import de.jl.notificationlog.databinding.AppListItemBinding
import de.jl.notificationlog.ui.AppsUtil
import kotlin.properties.Delegates

class AppListAdapter: RecyclerView.Adapter<AppListViewHolder>() {
    var items: List<AppListItem>? by Delegates.observable(null as List<AppListItem>?) {
        _, _, _ -> notifyDataSetChanged()
    }

    var selectedAppPackageName: String? by Delegates.observable(null as String?) {
        _, _, _ -> notifyDataSetChanged()
    }

    var listener: AppAdapterListener? = null

    init {
        setHasStableIds(true)
    }

    override fun getItemCount(): Int {
        val items = items

        if (items == null) {
            return 0
        } else {
            return items.size
        }
    }

    override fun getItemId(position: Int) = items!![position].getItemId()

    override fun getItemViewType(position: Int) = when(items!![position]) {
        MissingPermissionAppListItem -> AppListViewType.PermissionHeader
        NoDataAppListItem -> AppListViewType.EmptyView
        is AppEntryAppListItem -> AppListViewType.AppItem
        AllAppsListItem -> AppListViewType.AppItem
    }.ordinal

    override fun onCreateViewHolder(container: ViewGroup, viewType: Int) = when (viewType) {
        AppListViewType.EmptyView.ordinal -> {
            StaticItemViewHolder(
                    LayoutInflater.from(container.context)
                            .inflate(R.layout.app_list_empty, container, false)
            )
        }
        AppListViewType.PermissionHeader.ordinal -> {
            StaticItemViewHolder(
                    LayoutInflater.from(container.context)
                            .inflate(R.layout.app_list_permission_info, container, false)
                            .apply {
                                findViewById<View>(R.id.card_enable_service_button)
                                        .setOnClickListener {
                                            listener?.onShowPermissionViewClicked()
                                        }
                            }
            )
        }
        AppListViewType.AppItem.ordinal -> {
            AppViewHolder(
                    AppListItemBinding.inflate(
                            LayoutInflater.from(container.context),
                            container,
                            false
                    )
            )
        }
        else -> throw IllegalStateException()
    }

    override fun onBindViewHolder(viewHolder: AppListViewHolder, position: Int) {
        val item = items!![position]

        if (item is AppEntryAppListItem) {
            val holder = viewHolder as AppViewHolder

            holder.binding.title = item.title

            holder.binding.appIcon1.setImageDrawable(AppsUtil.getAppIcon(item.packageName, holder.itemView.context))
            holder.binding.appIcon2.setImageDrawable(AppsUtil.getAppIcon(item.packageName, holder.itemView.context))

            if (selectedAppPackageName == item.packageName) {
                holder.binding.flipper.displayedChild = 1
            } else {
                holder.binding.flipper.displayedChild = 0
            }

            holder.itemView.setOnClickListener {
                _ ->

                listener?.onAppClicked(item.packageName)
            }
        } else if (item is AllAppsListItem) {
            val holder = viewHolder as AppViewHolder

            holder.binding.title = holder.itemView.context.getString(R.string.merge_all_apps)

            holder.binding.appIcon1.setImageResource(R.mipmap.ic_app_list)
            holder.binding.appIcon2.setImageResource(R.mipmap.ic_app_list)

            if (selectedAppPackageName == AppListModel.ALL_APPS) {
                holder.binding.flipper.displayedChild = 1
            } else {
                holder.binding.flipper.displayedChild = 0
            }

            holder.itemView.setOnClickListener {
                _ ->

                listener?.onAllAppsClicked()
            }
        }
    }
}

interface AppAdapterListener {
    fun onAppClicked(packageName: String)
    fun onShowPermissionViewClicked()
    fun onAllAppsClicked()
}

enum class AppListViewType {
    EmptyView, PermissionHeader, AppItem
}

sealed class AppListViewHolder(view: View): RecyclerView.ViewHolder(view)
class StaticItemViewHolder(view: View): AppListViewHolder(view)
class AppViewHolder(val binding: AppListItemBinding): AppListViewHolder(binding.root)