package de.jl.notificationlog.ui.appdetail

import android.annotation.TargetApi
import android.app.Activity
import android.app.PendingIntent
import androidx.lifecycle.Observer
import androidx.paging.LivePagedListBuilder
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.*
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.paging.PagedList
import com.google.android.material.snackbar.Snackbar
import de.jl.notificationlog.R
import de.jl.notificationlog.data.AppDatabase
import de.jl.notificationlog.data.item.NotificationItem
import de.jl.notificationlog.databinding.AppDetailBinding
import de.jl.notificationlog.ui.AppsUtil
import de.jl.notificationlog.ui.SortSettingDialogFragment
import de.jl.notificationlog.ui.VersionHandlingSettingDialogFragment
import de.jl.notificationlog.ui.applist.AppListModel
import de.jl.notificationlog.util.Configuration
import de.jl.notificationlog.util.ExportAsyncTask
import de.jl.notificationlog.util.PendingIntentHolder

/**
 * A fragment representing a single App detail screen.
 * This fragment is either contained in a [AppListActivity]
 * in two-pane mode (on tablets) or a [AppDetailActivity]
 * on handsets.
 */
class AppDetailFragment : Fragment(), AppDetailAdapterListener {
    companion object {
        private const val ARG_PACKAGE_NAME = "packageName"
        private const val REQUEST_CHOSE_EXPORT_PATH = 1
        private const val REQUEST_CHANGE_CONFIG = 2

        fun newInstance(packageName: String) = AppDetailFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_PACKAGE_NAME, packageName)
            }
        }
    }

    val selectedPackageName: String by lazy { arguments!!.getString(ARG_PACKAGE_NAME) }
    val isLoggingEnabled = MutableLiveData<Boolean>()
    val pagedList = MutableLiveData<LiveData<PagedList<NotificationItem>>>()
    val pagedListContent = Transformations.switchMap(pagedList, { it })
    lateinit var binding: AppDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()

        isLoggingEnabled.value = Configuration.with(context!!).shouldLogNotifications(selectedPackageName)
    }

    fun updatePagedList() {
        val sorting = Configuration.with(context!!).sorting
        val versionHandling = Configuration.with(context!!).versionHandling

        pagedList.value = LivePagedListBuilder(when {
            selectedPackageName == AppListModel.ALL_APPS -> AppDatabase.with(context!!).notification().getNotificationsOfAllApps(sorting, versionHandling)
            else -> AppDatabase.with(context!!).notification().getNotificationsByApp(selectedPackageName, sorting, versionHandling)
        }, 20).build()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = AppDetailBinding.inflate(inflater, container, false)

        val adapter = AppDetailAdapter()
        adapter.listener = this

        updatePagedList()

        pagedListContent.observe(this, Observer {
            adapter.submitList(it)

            binding.isListEmpty = it != null && it.isEmpty()
        })

        isLoggingEnabled.observe(this, Observer {
            binding.loggingDisabled = selectedPackageName != AppListModel.ALL_APPS && !it
        })

        binding.recycler.adapter = adapter
        binding.appTitle = AppsUtil.getAppTitle(selectedPackageName, context!!)

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_app_detail, menu)

        menu.findItem(R.id.action_checkbox_enable_logging).apply {
            if (selectedPackageName == AppListModel.ALL_APPS) {
                isVisible = false
            } else {
                title = getString(R.string.action_checkbox_enable_logging, AppsUtil.getAppTitle(selectedPackageName, context!!))

                isLoggingEnabled.observe(this@AppDetailFragment, Observer {
                    isChecked = it
                })

                setOnMenuItemClickListener {
                    val newValue = !it.isChecked

                    Configuration.with(context!!).setShouldLogNotifications(selectedPackageName, newValue)
                    isLoggingEnabled.value = newValue

                    true
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when {
        item.itemId == R.id.action_export -> {
            startExport()

            true
        }
        item.itemId == R.id.action_clear_app -> {
            ClearDialogFragment.newInstance(selectedPackageName).show(fragmentManager!!)

            true
        }
        item.itemId == R.id.action_sort -> {
            SortSettingDialogFragment().apply {
                setTargetFragment(this@AppDetailFragment, REQUEST_CHANGE_CONFIG)
            }.show(fragmentManager!!)

            true
        }
        item.itemId == R.id.action_version -> {
            VersionHandlingSettingDialogFragment().apply {
                setTargetFragment(this@AppDetailFragment, REQUEST_CHANGE_CONFIG)
            }.show(fragmentManager!!)

            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private fun startExport() {
        startActivityForResult(
                Intent(Intent.ACTION_CREATE_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_TITLE, getString(R.string.app_name) + ".txt"),
                REQUEST_CHOSE_EXPORT_PATH
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(requestCode) {
            REQUEST_CHOSE_EXPORT_PATH -> {
                if (resultCode == Activity.RESULT_OK) {
                    ExportAsyncTask(activity!!.application, selectedPackageName, data!!.data!!).execute()
                }
            }
            REQUEST_CHANGE_CONFIG -> {
                updatePagedList()
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onNotificationClicked(savedNotificationId: Long) {
        if (Configuration.with(context!!).openNotifications) {
            openNotificationAfterConfirmation(savedNotificationId)
        } else {
            OpenNotificationInfoDialogFragment
                    .newInstance(savedNotificationId)
                    .apply {
                        setTargetFragment(this@AppDetailFragment, 0)
                    }
                    .show(fragmentManager!!)
        }
    }

    fun openNotificationAfterConfirmation(savedNotificationId: Long) {
        if (!tryOpenNotification(savedNotificationId)) {
            Snackbar.make(binding.recycler, R.string.open_notification_action_expired, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun tryOpenNotification(savedNotificationId: Long): Boolean {
        val pendingIntent = PendingIntentHolder.read(savedNotificationId)

        return if (pendingIntent == null) {
            false
        } else {
            try {
                pendingIntent.send()

                true
            } catch (ex: PendingIntent.CanceledException) {
                false
            }
        }
    }
}
