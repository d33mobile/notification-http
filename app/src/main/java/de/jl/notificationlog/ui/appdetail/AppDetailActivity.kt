package de.jl.notificationlog.ui.appdetail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.NavUtils
import android.view.MenuItem
import de.jl.notificationlog.R
import de.jl.notificationlog.ui.AppListActivity
import de.jl.notificationlog.ui.AppsUtil
import de.jl.notificationlog.ui.CheckAuthActivity
import de.jl.notificationlog.ui.applist.AppListModel
import kotlinx.android.synthetic.main.activity_app_detail.*

/**
 * An activity representing a single App detail screen. This
 * activity is only used on narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a [AppListActivity].
 */
class AppDetailActivity : CheckAuthActivity() {
    companion object {
        private const val EXTRA_PACKAGE_NAME = "packageName"

        fun newIntent(packageName: String, context: Context) = Intent(context, AppDetailActivity::class.java)
                .putExtra(EXTRA_PACKAGE_NAME, packageName)
    }

    private val selectedPackageName: String by lazy { intent.getStringExtra(EXTRA_PACKAGE_NAME)!! }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_detail)
        setSupportActionBar(detail_toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (selectedPackageName == AppListModel.ALL_APPS) {
            supportActionBar?.title = getString(R.string.merge_all_apps)
        } else {
            supportActionBar?.title = AppsUtil.getAppTitle(selectedPackageName, this)
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .add(R.id.app_detail_container, AppDetailFragment.newInstance(selectedPackageName))
                    .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) =
            when (item.itemId) {
                android.R.id.home -> {
                    NavUtils.navigateUpTo(this, Intent(this, AppListActivity::class.java))
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
}
