package de.jl.notificationlog.ui.appdetail

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import de.jl.notificationlog.R
import de.jl.notificationlog.util.Configuration

class OpenNotificationInfoDialogFragment: DialogFragment() {
    companion object {
        private const val DIALOG_TAG = "OpenNotificationInfoDialogFragment"
        private const val EXTRA_SAVED_NOTIFICATION_ID = "savedNotificationId"

        fun newInstance(savedNotificationId: Long) = OpenNotificationInfoDialogFragment().apply {
            arguments = Bundle().apply {
                putLong(EXTRA_SAVED_NOTIFICATION_ID, savedNotificationId)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog.Builder(context!!, theme)
            .setTitle(R.string.open_notification_info_title)
            .setMessage(R.string.open_notification_info_text)
            .setPositiveButton(R.string.open_notification_info_positive) { _, _ ->
                Configuration.with(context!!).openNotifications = true

                targetFragment?.let { target ->
                    if (target is AppDetailFragment) {
                        target.openNotificationAfterConfirmation(arguments!!.getLong(EXTRA_SAVED_NOTIFICATION_ID))
                    }
                }
            }
            .setNegativeButton(R.string.open_notification_info_negative, null)
            .create()

    fun show(fragmentManager: FragmentManager) = show(fragmentManager, DIALOG_TAG)
}