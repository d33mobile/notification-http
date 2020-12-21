package de.jl.notificationlog.ui.appdetail

import android.content.Context
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import de.jl.notificationlog.data.item.NotificationItem
import de.jl.notificationlog.databinding.NotificationItemBinding
import de.jl.notificationlog.ui.AppsUtil
import kotlin.properties.Delegates

class AppDetailAdapter: PagedListAdapter<NotificationItem, NotificationHolder>(
        object: DiffUtil.ItemCallback<NotificationItem>() {
            override fun areContentsTheSame(p0: NotificationItem, p1: NotificationItem) = p0 == p1
            override fun areItemsTheSame(p0: NotificationItem, p1: NotificationItem) = p0.id == p1.id
        }
) {
    companion object {
        fun formatTime(time: Long, context: Context) = DateUtils.formatDateTime(
                context,
                time,
                DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
        )
    }

    var listener: AppDetailAdapterListener? = null
    var showAppTitles: Boolean by Delegates.observable(false) { _, _, _ -> notifyDataSetChanged() }

    override fun onCreateViewHolder(container: ViewGroup, viewType: Int) = NotificationHolder(
        NotificationItemBinding.inflate(
                LayoutInflater.from(container.context),
                container,
                false
        )
    )

    override fun onBindViewHolder(holder: NotificationHolder, position: Int) {
        val item = getItem(position)

        if (item != null) {
            val baseTime = formatTime(item.time, holder.binding.root.context)
            val appTitle = AppsUtil.getAppTitle(item.packageName, holder.binding.root.context)
            val timeContent = if (showAppTitles) "$baseTime ($appTitle)" else baseTime

            holder.binding.title = item.title
            holder.binding.text = item.text
            holder.binding.time = timeContent
            holder.binding.progress = item.progress
            holder.binding.maxProgress = item.progressMax
            holder.binding.indeterminate = item.progressIndeterminate
            holder.binding.card.setOnClickListener { listener?.onNotificationClicked(item.id) }
            holder.binding.card.setOnLongClickListener { listener?.onNotificationLongClicked(item) ?: false }
        } else {
            holder.binding.title = ""
            holder.binding.text = ""
            holder.binding.time = ""
            holder.binding.progress = 0
            holder.binding.maxProgress = 0
            holder.binding.indeterminate = false
            holder.binding.card.setOnClickListener(null)
            holder.binding.card.setOnLongClickListener(null)
        }

        holder.binding.executePendingBindings()
    }
}

class NotificationHolder(val binding: NotificationItemBinding): RecyclerView.ViewHolder(binding.root)

interface AppDetailAdapterListener {
    fun onNotificationClicked(savedNotificationId: Long)
    fun onNotificationLongClicked(item: NotificationItem): Boolean
}