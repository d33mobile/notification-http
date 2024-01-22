package de.jl.notificationlog.ui.applist

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.map
import androidx.lifecycle.switchMap

import de.jl.notificationlog.data.AppDatabase
import de.jl.notificationlog.databinding.FragmentAppListBinding
import de.jl.notificationlog.livedata.ignoreUnchanged
import de.jl.notificationlog.ui.AppListActivity
import de.jl.notificationlog.ui.AppsUtil
import de.jl.notificationlog.ui.SortAppsSettingDialogFragment
import de.jl.notificationlog.util.Configuration
import de.jl.notificationlog.util.ServiceCheckUtil

class AppListFragment : Fragment() {
    companion object {
        private const val REQUEST_SORT_SETTING = 1
    }

    private val appListActivity: AppListActivity by lazy { activity as AppListActivity }
    private val model: AppListModel by lazy { appListActivity.model }
    private val hasRequiredPermission = MutableLiveData<Boolean>()
    private val appSorting = MutableLiveData<Configuration.AppSorting>()
    private val appSortingIgnoreUnchanged = appSorting.ignoreUnchanged()

    private val apps: LiveData<List<AppListItem>> by lazy {
        AppDatabase.with(requireContext()).notification().getAppsWithNotificationsUnsorted()
                .ignoreUnchanged()
                .switchMap { apps ->
                    appSortingIgnoreUnchanged.map { sorting ->
                        if (apps.isNotEmpty()) {
                            val appsEventuallySortedByTime = when (sorting!!) {
                                Configuration.AppSorting.Alphabetically -> apps
                                Configuration.AppSorting.NewestFirst -> apps.sortedByDescending { it.lastNotificationTimestamp }
                                Configuration.AppSorting.OldestFirst -> apps.sortedBy { it.lastNotificationTimestamp }
                            }

                            val appsWithTitle = appsEventuallySortedByTime.map { app ->
                                AppEntryAppListItem(
                                    packageName = app.packageName,
                                    title = AppsUtil.getAppTitle(app.packageName, requireContext())
                                )
                            }

                            if (sorting == Configuration.AppSorting.Alphabetically) {
                                appsWithTitle.sortedBy { app -> app.title.lowercase() }
                            } else appsWithTitle
                        } else {
                            listOf(NoDataAppListItem)
                        }
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

        hasRequiredPermission.value = ServiceCheckUtil.isNotificationReadingAllowed(requireContext())
        appSorting.value = Configuration.with(requireContext()).appSorting
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentAppListBinding.inflate(inflater, container, false)

        val adapter = AppListAdapter()

        listContent.observe(viewLifecycleOwner) { adapter.items = it }
        model.selectedPackageName.observe(viewLifecycleOwner) { adapter.selectedAppPackageName = it }

        adapter.listener = object: AppAdapterListener {
            override fun onAppClicked(packageName: String) {
                appListActivity.onAppSelected(packageName)
            }

            override fun onShowPermissionViewClicked() {
                ServiceCheckUtil.enableService(requireActivity())
            }

            override fun onAllAppsClicked() {
                appListActivity.onAppSelected(AppListModel.ALL_APPS)
            }
        }

        binding.recycler.adapter = adapter

        return binding.root
    }

    fun showSortSetting() {
        SortAppsSettingDialogFragment()
            .apply {
                setTargetFragment(this@AppListFragment, REQUEST_SORT_SETTING)
            }.show(parentFragmentManager)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_SORT_SETTING && resultCode == Activity.RESULT_OK) {
            appSorting.value = Configuration.with(requireContext()).appSorting
        }

        super.onActivityResult(requestCode, resultCode, data)
    }
}
