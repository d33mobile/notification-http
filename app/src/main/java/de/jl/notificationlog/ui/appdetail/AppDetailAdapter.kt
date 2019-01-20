package de.jl.notificationlog.ui.appdetail

import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import de.jl.notificationlog.data.item.NotificationItem
import de.jl.notificationlog.databinding.NotificationItemBinding

class AppDetailAdapter: PagedListAdapter<NotificationItem, NotificationHolder>(
        object: DiffUtil.ItemCallback<NotificationItem>() {
            override fun areContentsTheSame(p0: NotificationItem, p1: NotificationItem) = p0 == p1
            override fun areItemsTheSame(p0: NotificationItem, p1: NotificationItem) = p0.id == p1.id
        }
) {
    var listener: AppDetailAdapterListener? = null

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
            holder.binding.title = item.title
            holder.binding.text = item.text
            holder.binding.time = DateUtils.formatDateTime(
                    holder.itemView.context,
                    item.time,
                    DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
            )
            holder.binding.progress = item.progress
            holder.binding.maxProgress = item.progressMax
            holder.binding.indeterminate = item.progressIndeterminate
            holder.binding.card.setOnClickListener {
                listener?.onNotificationClicked(item.id)
            }
        } else {
            holder.binding.title = ""
            holder.binding.text = ""
            holder.binding.time = ""
            holder.binding.progress = 0
            holder.binding.maxProgress = 0
            holder.binding.indeterminate = false
            holder.binding.card.setOnClickListener(null)
        }

        holder.binding.executePendingBindings()
    }
}

class NotificationHolder(val binding: NotificationItemBinding): RecyclerView.ViewHolder(binding.root)

interface AppDetailAdapterListener {
    fun onNotificationClicked(savedNotificationId: Long)
}