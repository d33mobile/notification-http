package de.jl.notificationlog.ui.permission

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import de.jl.notificationlog.R

class RequestAccessibilityServiceDialogFragment: DialogFragment() {
    companion object {
        private const val DIALOG_TAG = "RequestAccessibilityServiceDialogFragment"

        fun newInstance() = RequestAccessibilityServiceDialogFragment()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog.Builder(requireContext(), theme)
        .setTitle(R.string.dialog_accessibility_service_title)
        .setMessage(R.string.dialog_accessibility_service_text)
        .setPositiveButton(R.string.dialog_accessibility_service_yes) { _, _ ->
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        .setNegativeButton(R.string.dialog_accessibility_service_no, null)
        .create()

    fun show(fragmentManager: FragmentManager) = show(fragmentManager, DIALOG_TAG)
}