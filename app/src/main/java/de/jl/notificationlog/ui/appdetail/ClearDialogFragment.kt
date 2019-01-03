package de.jl.notificationlog.ui.appdetail

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import de.jl.notificationlog.R
import de.jl.notificationlog.data.AppDatabase
import de.jl.notificationlog.ui.AppsUtil
import de.jl.notificationlog.ui.applist.AppListModel

class ClearDialogFragment : DialogFragment() {
    companion object {
        private const val TAG = "ClearDialogFragment"
        private const val ARGUMENT_PACKAGE = "package"

        fun newInstance(packageName: String) = ClearDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ARGUMENT_PACKAGE, packageName)
            }
        }
    }

    private val packageName: String by lazy { arguments!!.getString(ARGUMENT_PACKAGE) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity)
                .setTitle(
                        if (packageName == AppListModel.ALL_APPS)
                            getString(R.string.dialog_clear_all_title)
                        else
                            getString(R.string.dialog_clear_app_title, AppsUtil.getAppTitle(packageName, context!!))
                )
                .setMessage(R.string.dialog_clear_text)
                .setPositiveButton(R.string.dialog_clear_positive) { _, _ ->
                    val activity = activity!!

                    val database = AppDatabase.with(context!!)

                    Thread {
                        if (packageName == AppListModel.ALL_APPS) {
                            database.notification().deleteAllNotificationsSync()
                        } else {
                            database.notification().deleteNotificationsByAppSync(packageName)
                        }
                    }.start()

                    if (activity is AppDetailActivity) {
                        activity.finish()
                    }
                }
                .setNegativeButton(R.string.dialog_clear_negative, null).create()
    }

    fun show(fragmentManager: FragmentManager) {
        show(fragmentManager, TAG)
    }
}
