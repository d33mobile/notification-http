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

class VersionHandlingSettingDialogFragment: DialogFragment() {
    companion object {
        private const val TAG = "VersionHandlingSettingDialogFragment"
    }

    val config: Configuration
        get() = Configuration.with(context!!)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog.Builder(context!!, theme)
            .setTitle(R.string.version_handling_title)
            .setSingleChoiceItems(
                    arrayOf(
                            getString(R.string.version_handling_all),
                            getString(R.string.version_handling_oldest),
                            getString(R.string.version_handling_newest)
                    ),
                    when (config.versionHandling) {
                        Configuration.VersionHandling.ShowAllVersions -> 0
                        Configuration.VersionHandling.ShowOldestVersionOnly -> 1
                        Configuration.VersionHandling.ShowNewestVersionOnly -> 2
                    },
                    object: DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, item: Int) {
                            config.versionHandling = when (item) {
                                0 -> Configuration.VersionHandling.ShowAllVersions
                                1 -> Configuration.VersionHandling.ShowOldestVersionOnly
                                2 -> Configuration.VersionHandling.ShowNewestVersionOnly
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
