package de.jl.notificationlog.ui.applist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import de.jl.notificationlog.R
import de.jl.notificationlog.data.AppDatabase
import de.jl.notificationlog.livedata.ignoreUnchanged
import de.jl.notificationlog.livedata.map
import de.jl.notificationlog.livedata.switchMap
import de.jl.notificationlog.ui.AppListActivity
import de.jl.notificationlog.ui.AppsUtil
import de.jl.notificationlog.util.ServiceCheckUtil
import kotlinx.android.synthetic.main.fragment_app_list.*

class AppListFragment : Fragment() {
    private val appListActivity: AppListActivity by lazy { activity as AppListActivity }
    private val model: AppListModel by lazy { appListActivity.model }
    private val hasRequiredPermission = MutableLiveData<Boolean>()

    private val apps: LiveData<List<AppListItem>> by lazy {
        AppDatabase.with(context!!).notification().getAppsWithNotifications()
                .ignoreUnchanged()
                .map {
                    apps ->

                    if (apps.isNotEmpty()) {
                        apps.map { app ->

                            AppEntryAppListItem(
                                    packageName = app.packageName,
                                    title = AppsUtil.getAppTitle(app.packageName, context!!)
                            )
                        }.sortedBy { app -> app.title.toLowerCase() }
                    } else {
                        listOf(NoDataAppListItem)
                    }
                }
    }

    private val listContent: LiveData<ArrayList<AppListItem>> by lazy {
        apps.switchMap { apps ->

            hasRequiredPermission.map { hasRequiredPermission ->
                ArrayList<AppListItem>().apply {
                    if (!hasRequiredPermission) {
                        add(MissingPermissionAppListItem)
                    }
                    add(AllAppsListItem)
                    addAll(apps)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        hasRequiredPermission.value = ServiceCheckUtil.isNotificationReadingAllowed(context!!)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_app_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = AppListAdapter()

        listContent.observe(this, Observer { adapter.items = it })
        model.selectedPackageName.observe(this, Observer { adapter.selectedAppPackageName = it })

        adapter.listener = object: AppAdapterListener {
            override fun onAppClicked(packageName: String) {
                appListActivity.onAppSelected(packageName)
            }

            override fun onShowPermissionViewClicked() {
                ServiceCheckUtil.enableService(context!!)
            }

            override fun onAllAppsClicked() {
                appListActivity.onAppSelected(AppListModel.ALL_APPS)
            }
        }

        recycler.adapter = adapter
    }
}
