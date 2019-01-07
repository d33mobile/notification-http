package de.jl.notificationlog.ui.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.jl.notificationlog.databinding.CheckableAppListItemBinding
import de.jl.notificationlog.ui.App
import de.jl.notificationlog.ui.AppsUtil
import kotlin.properties.Delegates

class CheckableAppListAdapter: RecyclerView.Adapter<ViewHolder>() {
    var apps: List<App> by Delegates.observable(emptyList()) { _, _, _ -> notifyDataSetChanged() }
    var checkedPackageNames: Set<String> by Delegates.observable(emptySet()) { _, _, _ -> notifyDataSetChanged() }
    var listener: Listener? = null

    init {
        setHasStableIds(true)
    }

    override fun getItemCount(): Int = apps.size
    override fun getItemId(position: Int): Long = apps[position].packageName.hashCode().toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
            CheckableAppListItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
            )
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = apps[position]
        val isChecked = checkedPackageNames.contains(item.packageName)

        holder.binding.title = item.title
        holder.binding.appIcon.setImageDrawable(AppsUtil.getAppIcon(item.packageName, holder.itemView.context))
        holder.binding.isChecked = isChecked
        holder.binding.root.setOnClickListener { listener?.onAppClicked(item) }
    }
}

class ViewHolder(val binding: CheckableAppListItemBinding): RecyclerView.ViewHolder(binding.root)

interface Listener {
    fun onAppClicked(app: App)
}
