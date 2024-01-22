package de.jl.notificationlog.ui

import androidx.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.Menu
import android.view.MenuItem
import android.view.View
import de.jl.notificationlog.R
import de.jl.notificationlog.ui.about.AboutActivity
import de.jl.notificationlog.ui.appdetail.AppDetailActivity
import de.jl.notificationlog.ui.appdetail.AppDetailFragment
import de.jl.notificationlog.ui.applist.AppListFragment
import de.jl.notificationlog.ui.applist.AppListModel
import de.jl.notificationlog.ui.settings.SettingsActivity

/**
 * An activity representing a list of Apps with Notifications. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a [AppDetailActivity] representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
class AppListActivity : CheckAuthActivity() {
    companion object {
        private const val STATE_SELECTED_PACKAGE = "packageName"
    }

    val model: AppListModel by lazy { ViewModelProviders.of(this).get(AppListModel::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_list)

        setSupportActionBar(findViewById(R.id.toolbar))

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.app_list_container, AppListFragment())
                    .commitNow()
        } else {
            model.selectedPackageName.value = savedInstanceState.getString(STATE_SELECTED_PACKAGE)
        }

        model.isTwoPaneMode.value = findViewById<View?>(R.id.app_detail_container) != null

        if (model.isTwoPaneMode.value!!) {
            val fragment = supportFragmentManager.findFragmentById(R.id.app_detail_container) as AppDetailFragment?
            val selectedPackageName = model.selectedPackageName.value

            if (selectedPackageName == null && fragment != null) {
                supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.app_detail_container, null as Fragment)
                        .commit()
            } else if (fragment != null && fragment.selectedPackageName != selectedPackageName) {
                supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.app_detail_container, AppDetailFragment.newInstance(packageName))
                        .commit()
            }
        } else {
            model.selectedPackageName.value = null
        }
    }

    fun onAppSelected(packageName: String) {
        if (model.isTwoPaneMode.value!!) {
            if (model.selectedPackageName.value != packageName) {
                supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.app_detail_container, AppDetailFragment.newInstance(packageName))
                        .commit()

                model.selectedPackageName.value = packageName
            }
        } else {
            startActivity(AppDetailActivity.newIntent(packageName, this))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(STATE_SELECTED_PACKAGE, model.selectedPackageName.value)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        menu!!.findItem(R.id.action_require_auth).let { option ->
            option.isChecked = configuration.requireAuth
            option.isVisible = CheckAuthUtil.SUPPORTED
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when {
        item.itemId == R.id.action_about -> {
            startActivity(Intent(this, AboutActivity::class.java))

            true
        }
        item.itemId == R.id.action_settings -> {
            startActivity(Intent(this, SettingsActivity::class.java))

            true
        }
        item.itemId == R.id.action_auto_delete -> {
            DeleteOldNotificationsDialog().show(supportFragmentManager)

            true
        }
        item.itemId == R.id.action_require_auth -> {
            val newValue = !item.isChecked

            configuration.requireAuth = newValue
            item.isChecked = newValue

            if (newValue) {
                authUtil.isAuthenticated = true
            }

            true
        }
        item.itemId == R.id.action_sort_apps -> {
            (supportFragmentManager.findFragmentById(R.id.app_list_container) as AppListFragment)
                .showSortSetting()

            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
