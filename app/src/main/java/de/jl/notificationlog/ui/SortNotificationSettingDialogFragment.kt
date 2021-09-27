package de.jl.notificationlog.ui

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import de.jl.notificationlog.R
import de.jl.notificationlog.util.Configuration

class SortNotificationSettingDialogFragment: DialogFragment() {
    companion object {
        private const val TAG = "SortNotificationSettingDialogFragment"
    }

    val config: Configuration
        get() = Configuration.with(requireContext())

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog.Builder(context!!, theme)
            .setTitle(R.string.sorting_notifications_title)
            .setSingleChoiceItems(
                    arrayOf(
                            getString(R.string.sort_oldest_first),
                            getString(R.string.sort_newest_first)
                    ),
                    when (config.notificationSorting) {
                        Configuration.NotificationSorting.OldestFirst -> 0
                        Configuration.NotificationSorting.NewestFirst -> 1
                    },
                    object: DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, item: Int) {
                            config.notificationSorting = when (item) {
                                0 -> Configuration.NotificationSorting.OldestFirst
                                1 -> Configuration.NotificationSorting.NewestFirst
                                else -> throw IllegalArgumentException()
                            }

                            targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, null)
                            dismiss()
                        }
                    }
            )
            .create()

    fun show(fragmentManager: FragmentManager) = show(fragmentManager, TAG)
}
