package de.jl.notificationlog.ui.appdetail

import android.annotation.TargetApi
import android.app.Activity
import androidx.lifecycle.Observer
import androidx.paging.LivePagedListBuilder
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.*
import androidx.lifecycle.MutableLiveData
import de.jl.notificationlog.R
import de.jl.notificationlog.data.AppDatabase
import de.jl.notificationlog.databinding.AppDetailBinding
import de.jl.notificationlog.ui.AppsUtil
import de.jl.notificationlog.ui.applist.AppListModel
import de.jl.notificationlog.util.Configuration
import de.jl.notificationlog.util.ExportAsyncTask

/**
 * A fragment representing a single App detail screen.
 * This fragment is either contained in a [AppListActivity]
 * in two-pane mode (on tablets) or a [AppDetailActivity]
 * on handsets.
 */
class AppDetailFragment : Fragment() {
    companion object {
        private const val ARG_PACKAGE_NAME = "packageName"
        private const val REQUEST_CHOSE_EXPORT_PATH = 1

        fun newInstance(packageName: String) = AppDetailFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_PACKAGE_NAME, packageName)
            }
        }
    }

    val selectedPackageName: String by lazy { arguments!!.getString(ARG_PACKAGE_NAME) }
    val isLoggingEnabled = MutableLiveData<Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()

        isLoggingEnabled.value = Configuration.with(context!!).shouldLogNotifications(selectedPackageName)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = AppDetailBinding.inflate(inflater, container, false)

        val adapter = AppDetailAdapter()

        LivePagedListBuilder(when {
            selectedPackageName == AppListModel.ALL_APPS -> AppDatabase.with(context!!).notification().getNotificationsOfAllApps()
            else -> AppDatabase.with(context!!).notification().getNotificationsByApp(selectedPackageName)
        }, 20)
                .build()
                .observe(this, Observer {
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
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
