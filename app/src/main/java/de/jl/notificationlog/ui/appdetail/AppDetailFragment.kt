package de.jl.notificationlog.ui.appdetail

import android.annotation.TargetApi
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.*
import com.google.android.material.snackbar.Snackbar
import de.jl.notificationlog.R
import de.jl.notificationlog.data.AppDatabase
import de.jl.notificationlog.data.item.NotificationItem
import de.jl.notificationlog.databinding.AppDetailBinding
import de.jl.notificationlog.ui.AppsUtil
import de.jl.notificationlog.ui.SortNotificationSettingDialogFragment
import de.jl.notificationlog.ui.VersionHandlingSettingDialogFragment
import de.jl.notificationlog.ui.applist.AppListModel
import de.jl.notificationlog.util.Configuration
import de.jl.notificationlog.util.ExportAsyncTask
import de.jl.notificationlog.util.PendingIntentHolder
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.*
import androidx.paging.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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

    val selectedPackageName: String by lazy { requireArguments().getString(ARG_PACKAGE_NAME)!! }
    private val isLoggingEnabled = MutableLiveData<Boolean>()
    private val isDeduplicationEnabled = MutableLiveData<Boolean>()
    private val pagedList = MutableLiveData<Pager<Int, NotificationItem>>()
    lateinit var binding: AppDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()

        isLoggingEnabled.value = Configuration.with(requireContext()).shouldLogNotifications(selectedPackageName)
        isDeduplicationEnabled.value = Configuration.with(requireContext()).hideDuplicates
    }

    private fun updatePagedList() {
        val sorting = Configuration.with(requireContext()).notificationSorting
        val versionHandling = Configuration.with(requireContext()).versionHandling
        val hideDuplicates = Configuration.with(requireContext()).hideDuplicates

        pagedList.value = Pager(
            PagingConfig(20),
            null,
            AppDatabase.with(requireContext()).notification().getNotifications(
                if (selectedPackageName == AppListModel.ALL_APPS) null else selectedPackageName,
                sorting, versionHandling, hideDuplicates
            ).asPagingSourceFactory(Dispatchers.IO)
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = AppDetailBinding.inflate(inflater, container, false)

        val adapter = AppDetailAdapter()

        adapter.listener = this
        adapter.showAppTitles = selectedPackageName == AppListModel.ALL_APPS

        updatePagedList()

        lifecycleScope.launch {
            pagedList.asFlow().collectLatest { pagedList ->
                pagedList.flow.collectLatest { data ->
                    adapter.submitData(data)
                }
            }
        }

        isLoggingEnabled.observe(viewLifecycleOwner, Observer {
            binding.loggingDisabled = selectedPackageName != AppListModel.ALL_APPS && !it
        })

        binding.recycler.adapter = adapter
        binding.appTitle = AppsUtil.getAppTitle(selectedPackageName, requireContext())

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_app_detail, menu)

        menu.findItem(R.id.action_checkbox_enable_logging).apply {
            if (selectedPackageName == AppListModel.ALL_APPS) {
                isVisible = false
            } else {
                title = getString(R.string.action_checkbox_enable_logging, AppsUtil.getAppTitle(selectedPackageName, requireContext()))

                isLoggingEnabled.observe(this@AppDetailFragment, Observer {
                    isChecked = it
                })

                setOnMenuItemClickListener {
                    val newValue = !it.isChecked

                    Configuration.with(requireContext()).setShouldLogNotifications(selectedPackageName, newValue)
                    isLoggingEnabled.value = newValue

                    true
                }
            }
        }

        menu.findItem(R.id.action_hide_duplicates).apply {
            isDeduplicationEnabled.observe(viewLifecycleOwner, Observer {
                isChecked = it
            })

            setOnMenuItemClickListener {
                val newValue = !it.isChecked

                Configuration.with(requireContext()).hideDuplicates = newValue
                isDeduplicationEnabled.value = newValue

                updatePagedList()

                true
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when {
        item.itemId == R.id.action_export -> {
            startExport()

            true
        }
        item.itemId == R.id.action_clear_app -> {
            ClearDialogFragment.newInstance(selectedPackageName).show(parentFragmentManager)

            true
        }
        item.itemId == R.id.action_sort -> {
            SortNotificationSettingDialogFragment().apply {
                setTargetFragment(this@AppDetailFragment, REQUEST_CHANGE_CONFIG)
            }.show(parentFragmentManager)

            true
        }
        item.itemId == R.id.action_version -> {
            VersionHandlingSettingDialogFragment().apply {
                setTargetFragment(this@AppDetailFragment, REQUEST_CHANGE_CONFIG)
            }.show(parentFragmentManager)

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
                    ExportAsyncTask(requireActivity().application, selectedPackageName, data!!.data!!).execute()
                }
            }
            REQUEST_CHANGE_CONFIG -> {
                updatePagedList()
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onNotificationClicked(savedNotificationId: Long) {
        if (Configuration.with(requireContext()).openNotifications) {
            openNotificationAfterConfirmation(savedNotificationId)
        } else {
            OpenNotificationInfoDialogFragment
                    .newInstance(savedNotificationId)
                    .apply {
                        setTargetFragment(this@AppDetailFragment, 0)
                    }
                    .show(parentFragmentManager)
        }
    }

    override fun onNotificationLongClicked(item: NotificationItem): Boolean {
        Snackbar.make(binding.recycler, R.string.copy_to_clipboard_toast, Snackbar.LENGTH_SHORT).show()

        (requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                ClipData.newPlainText(
                        "notification",
                        "${AppDetailAdapter.formatTime(item.time, requireContext())}: ${item.title} / ${item.text}"
                )
        )

        return true
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
