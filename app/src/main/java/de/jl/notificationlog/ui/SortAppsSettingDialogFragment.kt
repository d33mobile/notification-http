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

class SortAppsSettingDialogFragment: DialogFragment() {
    companion object {
        private const val TAG = "SortAppsSettingDialogFragment"
    }

    val config: Configuration
        get() = Configuration.with(requireContext())

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog.Builder(requireContext(), theme)
            .setTitle(R.string.sorting_apps_title)
            .setSingleChoiceItems(
                    arrayOf(
                        getString(R.string.sort_alphabetically),
                        getString(R.string.sort_newest_first),
                        getString(R.string.sort_oldest_first),
                    ),
                    when (config.appSorting) {
                        Configuration.AppSorting.Alphabetically -> 0
                        Configuration.AppSorting.NewestFirst -> 1
                        Configuration.AppSorting.OldestFirst -> 2
                    },
                    object: DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, item: Int) {
                            config.appSorting = when (item) {
                                0 -> Configuration.AppSorting.Alphabetically
                                1 -> Configuration.AppSorting.NewestFirst
                                2 -> Configuration.AppSorting.OldestFirst
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
