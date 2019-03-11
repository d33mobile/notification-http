package de.jl.notificationlog.ui

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import de.jl.notificationlog.R
import de.jl.notificationlog.util.Configuration
import de.jl.notificationlog.util.DeleteOldNotificationsUtil

class DeleteOldNotificationsDialog: DialogFragment() {
    companion object {
        private const val TAG = "DeleteOldNotificationsDialog"
        private val OPTIONS = listOf(0, 1, 2, 3, 4, 5, 6, 7)
    }

    val config: Configuration
        get() = Configuration.with(context!!)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(context!!, theme)
                .setTitle(R.string.delete_old_notifications_title)
                .setSingleChoiceItems(
                        OPTIONS.map { option ->
                            if (option == 0) {
                                getString(R.string.delete_old_notifications_never)
                            } else {
                                resources.getQuantityString(R.plurals.delete_old_notifications_days, option, option)
                            }
                        }.toTypedArray(),
                        OPTIONS.indexOf(config.notificationKeepingDays),
                        object: DialogInterface.OnClickListener {
                            override fun onClick(dialog: DialogInterface?, item: Int) {
                                config.notificationKeepingDays = OPTIONS[item]

                                DeleteOldNotificationsUtil.with(context!!).cleanupDatabaseAsync()

                                dismiss()
                            }
                        }
                )
                .create()
    }

    fun show(fragmentManager: FragmentManager) = show(fragmentManager, TAG)
}