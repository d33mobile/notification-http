package de.jl.notificationlog.ui.applist

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AppListModel: ViewModel() {
    companion object {
        const val ALL_APPS = ":all"
    }

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    val isTwoPaneMode = MutableLiveData<Boolean>().apply { value = false }

    val selectedPackageName = MutableLiveData<String?>().apply { value = null }
}
